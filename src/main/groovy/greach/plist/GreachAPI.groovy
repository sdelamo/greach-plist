package greach.plist

import groovy.json.JsonSlurper

class GreachAPI {
    private static final String ROOT_API_URL = 'http://greach.botconf.com/wp-json/posts'
    private static final String POST_TYPE_TALK = 'talk'
    private static final String POST_TYPE_SPEAKER = 'speaker'
    private static final int POST_PER_PAGE = 100
    private static final String API_AGENDA_PATH = 'agenda'
    private static final String API_TALKS_PATH = 'talks'

    def jsonSlurper = new JsonSlurper()
    def tracks
    def people
    def sessions
    def rooms = [[name: 'Room 1', image: ''], [name: 'Room 4', image: '']]

    String buildUrl(String postType, int postPerPage)  {
        "${ROOT_API_URL}?type=${postType}&filter[posts_per_page]=${postPerPage}"
    }

    def extractPresenters = { speakers, allspeakers ->
        def presenters = []
        speakers.each { speaker ->
            def foundSpeaker = allspeakers.find {it.peopleId == speaker.id}
            if(foundSpeaker) {
                presenters << foundSpeaker.twitter
            }
        }
        presenters
    }

    def stripHtml = { html ->
        html = html.replaceAll('<p>','')
        html = html.replaceAll('</p>','')
        html = html.replaceAll('<br />','')
        html = html.replaceAll('<br/>','')
        html = html.replaceAll('&#8217;','\'')
        html
    }

    def lastName = { fullname ->
        def tokens = fullname.tokenize(' ')
        def arr = []
        if(tokens.size() > 1) {
            for(int i=1;i<tokens.size();i++) {
                arr << tokens[i]
            }
        }
        arr.join(' ')
    }

    def trackIdForTalk = { talk ->

        def trackIds = []
        if(talk.terms instanceof Map) {
            talk.terms.each { k, v ->
                if(k == 'category') {
                    v.each { category ->
                        trackIds << category.ID

                    }
                }
            }
        }

        !trackIds.isEmpty() ? trackIds[0] : null
    }

    def trackNameForTalk = { talk ->

        def trackNames = []
        if(talk.terms instanceof Map) {
            talk.terms.each { k, v ->
                if(k == 'category') {
                    v.each { category ->
                        trackNames << category.name

                    }
                }
            }
        }

        !trackNames.isEmpty() ? trackNames[0] : null
    }

    def starDateForTalk = { agendaObject, talkId ->
        for(int i = 0; i < agendaObject.size();i++) {
            def day = agendaObject[i].day

            for(def track in agendaObject[i].tracks) {
                for(def slot in track.slots) {
                    if(slot.talk.id == talkId) {
                        return Date.parse('yyyy-MM-dd HH:mm:ss', "${day} ${slot.start}")
                    }
                }
            }
        }
        return null
    }


    def calculateRoomId = { rooms, room ->


        for(int i = 0; i < rooms.size();i++) {
            if(rooms[i] == room) {
                return i
            }
        }
    }


    def calculateTrackIdByName = { tracks, trackName ->
        for(int i = 0; i < tracks.size();i++) {
            if(trackName.startsWith(tracks[i])) {
                return i
            }
        }
    }

    def calculateTrackId = { tracks, trackId ->
        for(int i = 0; i < tracks.size();i++) {
            if(tracks[i].id == trackId) {
                return i
            }
        }
    }


    def extractData() {

        println 'Extracting people'
        def speakersObject = fetchSpeakersObject()
        extractPeople(speakersObject)
        println 'Extracting tracks'
        def talksObject = fetchTalksObject()

        talksObject = talksObject.findAll {
            Date startDate = startDateForPost(it)
            def d = new Date()
            use(groovy.time.TimeCategory) {
                d = d - 7.day
            }
            startDate.after(d)
        }

        extractTracks(talksObject)

        tracks = tracks.collect { it.name.replaceAll('.0','').trim() }.unique()
        println tracks

        println 'Extracting Session'
        sessions = extractSessions(talksObject)
        println sessions.collect { [title: it.title, presenters: it.presenters] }

        println "Speakers without twitter ${people.findAll { !it.twitter }.size()}"
        println people.findAll { !it.twitter }.collect { "${it.first} ${it.last}" }
    }

    def extractPeople(def object) {
        people = object.collect {
            def twitter = (it.acf instanceof Map && it.acf.twitter instanceof String) ? it.acf.twitter : ''
            [peopleId: it.ID,
             first: it.title.tokenize(' ')[0],
             last:lastName(it.title),
             twitter: twitter,
             bio: stripHtml(it.content),
             active: true] }
    }

    def fetchTalksObject() {
        def urlStr = buildUrl(POST_TYPE_TALK, POST_PER_PAGE)
        def text = new URL(urlStr).text
        jsonSlurper.parseText(text)
    }

    def fetchSpeakersObject() {
        def urlStr = buildUrl(POST_TYPE_SPEAKER, POST_PER_PAGE)
        def text = new URL(urlStr).text
        jsonSlurper.parseText(text)
    }

    def extractSessions(def talksObject) {

        talksObject.collect {
            def speakers = extractSpeakersForPost(it)
            def presenters = speakers.collect {
                def speaker = people.find { p -> p.peopleId == it.id}
                speaker?.twitter
            }

            Date startDate = startDateForPost(it)
            Date endDate = endDateForPost(it)

            def minutes
            use(groovy.time.TimeCategory) {
                def duration = endDate - startDate
                minutes = duration.minutes
            }
            def trackId = calculateTrackIdByName(tracks, trackNameForTalk(it))
            println "TrackId: " + trackId + " " + tracks[trackId]

            def roomId = null
            if(tracks[trackId] == 'Track 1') {
                roomId = 0

            } else if(tracks[trackId] == 'Track 2') {
                roomId = 1
            }

            [
             active: true,
             date: startDate,
             duration: minutes,
             trackId: trackId,
             column: 1,
             sessionNumber: it.ID,
             title: stripHtml(it.title),
             sessionDescription: stripHtml(it.content),
             presenters:presenters,
             roomId: roomId
             ]
        }
    }

    Date startDateForPost(def talk) {
        Date startDate
        talk.acf.each { k, v ->
            if (k == 'start_time') {
                startDate = Date.parse('dd/MM/yyyy HH:mm', v)
            }
        }
        startDate
    }

    Date endDateForPost(def talk) {
        Date endDate
        talk.acf.each { k, v ->
            if (k == 'end_time') {
                endDate = Date.parse('dd/MM/yyyy HH:mm', v)
            }
        }
        endDate
    }

    def extractSpeakersForPost(def talk) {
        def speakers = []
        talk.acf.each { k, v ->
            if(k == 'speaker') {
                v.each { speaker ->
                    if(speaker instanceof Map && speaker.containsKey('ID')) {
                        speakers << [id: speaker.ID]
                    }
                }
            }
        }
        speakers
    }

    def extractTracks(def object) {

        def tracks = []
        object.each { talk ->
            talk.terms.each { k, v ->
                if(k == 'category') {
                    v.each { category ->
                        tracks << [name: category.name, id: category.ID]

                    }
                }
            }

        }
        this.tracks = tracks.unique()
    }


}
