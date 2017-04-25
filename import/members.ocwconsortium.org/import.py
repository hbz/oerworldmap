__author__ = 'fo'

import getopt
import json
import os
import pycurl
import sys
import time
import urllib
import urllib2
from StringIO import StringIO


def convert(output_dir):
    print 'Output dir is ', output_dir
    members = list_members()
    feature_collection = get_feature_collection()
    print 'Read feature collection'
    for key, value in members.iteritems():
        total = len (value)
        for idx, member in enumerate(value):
            print "Processing " + str(idx+1) + " of " + str(total) + " entries in " + key
            oerwm_id = get_uri(member["id"])
            details = get_details(member["id"])
            data = {
                '@context': 'http://schema.org/',
                '@id': oerwm_id,
                '@type': 'Organization',
                'name': [
                    {
                        '@language': 'en',
                        '@value': details['name']
                    }
                ],
                'description': [
                    {
                        '@language': 'en',
                        '@value': details['description']
                    }
                ],
                'url': details['main_website'],
                'sameAs': ["http://www.oeconsortium.org/members/view/" + str(member["id"]) + "/"],
                'memberOf': [
                    {
                        "@id": "urn:uuid:ff56c436-7e76-11e5-b76b-54ee7558c81f"
                    }
                ]
            }

            try:
                data['image'] = 'http://www.oeconsortium.org/media/' + urllib.quote(details['logo_small'])
            except KeyError:
                pass

            coordinates = get_coordinates(member["id"], feature_collection)
            location = get_location(coordinates)
            if location is not None:
                data['location'] = location

            output_path = os.path.join(output_dir, oerwm_id)
            with open(output_path, 'w') as file:
                json.dump(data, file)
            print "Wrote data for " + oerwm_id

def get_uri(oec_id):
    with open("id_map.json", 'r') as file:
        ids = json.loads(file.read())
        return ids[str(oec_id)]

def list_members():
    url = "http://members.oeconsortium.org/api/v1/organization/group_by/membership_type/list/?format=json"
    print "Got member list"
    return json.loads(get_url(url))

def get_details(member_id):
    url = "http://members.oeconsortium.org/api/v1/organization/view/" + str(member_id) + "/?format=json"
    print "Got member detail for " + str(member_id)
    return json.loads(get_url(url))

def get_feature_collection():
    url = "http://members.oeconsortium.org/api/v1/address/list/geo/?format=json"
    print "Got feature collection"
    return json.loads(get_url(url))

def get_coordinates(member_id, feature_collection):
    for feature in feature_collection["features"]:
        if (feature["id"] == member_id):
            return feature["geometry"]["coordinates"]

def get_location(coordinates):
    if coordinates is None:
        return None
    time.sleep(1)
    lon = coordinates[0]
    lat = coordinates[1]
    headers = { 'User-Agent' : 'Mozilla/5.0' }
    url = "http://nominatim.openstreetmap.org/reverse?format=json&lat=" + str(lat) + "&lon=" + str(lon) + "&addressdetails=1&accept-language=en"
    req = urllib2.Request(url, None, headers)
    response = urllib2.urlopen(req)
    address = json.loads(response.read())['address']
    data = {
        '@type': 'Place',
        'geo': {
            'lat': lat,
            'lon': lon
        },
        'address': {
            '@type': 'PostalAddress'
        }
    }
    if 'country_code' in address:
        data['address']['addressCountry'] = address['country_code'].upper()
    if 'city' in address:
        data['address']['addressLocality'] = address['city']
    if 'postcode' in address:
        data['address']['postalCode'] = address['postcode']
    if 'road' in address:
        data['address']['streetAddress'] = address['road']
    if 'house' in address and 'road' in address:
        data['address']['streetAddress'] = address['house'] + ' ' + address['road']
    return data

def get_url(url):
    buffer = StringIO()
    c = pycurl.Curl()
    c.setopt(c.URL, url)
    c.setopt(c.WRITEDATA, buffer)
    c.perform()
    c.close()
    return buffer.getvalue()

if __name__ == "__main__":
    output_dir = ''
    try:
        opts, args = getopt.getopt(sys.argv[1:],"ho:",["odir="])
    except getopt.GetoptError:
        print 'convert.py -o <outputdir>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print 'convert.py -o <outputdir>'
            sys.exit()
        elif opt in ("-o", "--odir"):
            output_dir = arg
    convert(output_dir)
