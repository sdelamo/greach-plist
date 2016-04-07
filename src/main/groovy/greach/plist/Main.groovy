package greach.plist

def api = new GreachAPI()
api.extractData()

def pListGenerator = new PlistGenerator()
pListGenerator.savePlist(api.tracks, api.rooms.collect { it.name }, api.people, api.sessions)