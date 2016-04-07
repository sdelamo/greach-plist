package greach.plist

import com.dd.plist.NSArray
import com.dd.plist.NSData
import com.dd.plist.NSDate
import com.dd.plist.NSDictionary
import com.dd.plist.NSString
import com.dd.plist.PropertyListParser

class PlistGenerator {

    static final String PLIST_NAME =  'greach-2016.plist'

    void savePlist(List<Map> trackNames, List<String> roomNames, List<Map> peopleMapList, List<Map> sessionsMapList) {

        try {
            NSDictionary root = new NSDictionary();

            NSDictionary metadata = new NSDictionary()

            metadata.put("lastUpdated", new NSDate(NSDate.makeDateString(new Date())))
            root.put("metadata", metadata);


            NSDictionary sessions = new NSDictionary()

            sessionsMapList.each { map ->
                NSDictionary sessionDict = new NSDictionary()
                sessionDict.put('active', map.active)
                sessionDict.put('date', map.date)
                sessionDict.put('duration', map.duration)
                sessionDict.put('trackId', map.trackId)
                sessionDict.put('column', map.column)
                sessionDict.put('sessionNumber', map.sessionNumber)
                sessionDict.put('title', map.title)
                sessionDict.put('sessionDescription', map.sessionDescription)
                sessionDict.put('presenters', map.presenters)
                sessionDict.put('roomId', map.roomId)
                sessions.put("${roomNames[map.roomId][0].toLowerCase()}${map.sessionNumber}", sessionDict)
            }
            root.put("sessions", sessions);

            NSDictionary people = new NSDictionary()

            peopleMapList.each { map ->
                NSDictionary speakerDict = new NSDictionary()
                speakerDict.put('active', map.active)
                speakerDict.put('first', map.first)
                speakerDict.put('last', map.last)
                speakerDict.put('twitter', map.twitter)
                speakerDict.put('bio', map.bio)
                people.put(map.twitter, speakerDict)
            }

            root.put("people", people);

            NSArray rooms = new NSArray(roomNames.size())
            for(int i=0;i < roomNames.size();i++) {
                NSDictionary roomDict = new NSDictionary()
                roomDict.put('name',roomNames[i])
                roomDict.put('image','')
                rooms.array[i] = roomDict
            }
            root.put("rooms", rooms);

            def tracks = new NSArray(trackNames.size())
            for(int i = 0; i < trackNames.size(); i++) {
                if(trackNames[i] instanceof String) {
                    tracks.setValue(i, trackNames[i])

                } else if(trackNames[i] instanceof Map) {
                    tracks.setValue(i, trackNames[i].name)
                }
            }
            root.put("tracks", tracks);

            def f = new File(PLIST_NAME)
            PropertyListParser.saveAsXML(root, f.newOutputStream());

        } catch(Exception ex) {
            println ex.getMessage()
        }

    }

}
