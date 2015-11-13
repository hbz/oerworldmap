__author__ = 'fo'

import sys, getopt, json, os, urllib2, time, uuid, socket, pycurl
from StringIO import StringIO

def list_members():
    url = "http://members.oeconsortium.org/api/v1/organization/group_by/membership_type/list/?format=json"
    print "Got member list"
    return json.loads(get_url(url))

def get_url(url):
    buffer = StringIO()
    c = pycurl.Curl()
    c.setopt(c.URL, url)
    c.setopt(c.WRITEDATA, buffer)
    c.perform()
    c.close()
    return buffer.getvalue()

if __name__ == "__main__":

    id_map = {}
    with open("id_map.json", 'r') as file:
        id_map = json.loads(file.read())
        print id_map

    for key, value in list_members().iteritems():
        for idx, member in enumerate(value):
            if str(member["id"]) in id_map:
                oerwm_id = id_map[str(member["id"])]
                print "Mapped " + str(member["id"]) + " to existing " + oerwm_id
            else:
                oerwm_id = 'urn:uuid:' + str(uuid.uuid1())
                id_map[member["id"]] = oerwm_id
                print "Mapped " + str(member["id"]) + " to " + oerwm_id

    with open("id_map.json", 'w') as file:
        json.dump(id_map, file)
