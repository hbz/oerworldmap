__author__ = 'fo'

import sys, getopt, json, os, urllib2, time, uuid

def convert(output_dir):
    print 'Output dir is ', output_dir
    members = list_members()
    feature_collection = get_feature_collection()
    for key, value in members.iteritems():
        total = len (value)
        for idx, member in enumerate(value):
            print "Processing " + str(idx+1) + " of " + str(total) + " entries in " + key
            oerwm_id = 'urn:uuid:' + str(uuid.uuid1())
            details = get_details(member["id"])
            data = {
                '@context': 'http://schema.org/',
                '@id': oerwm_id,
                '@type': 'Organization',
                'legalName': {
                    '@language': 'en',
                    '@value': details['name']
                },
                'description': {
                    '@language': 'en',
                    '@value': details['description']
                },
                'url': details['main_website'],
            }

            coordinates = get_coordinates(member["id"], feature_collection)
            location = get_location(coordinates)
            if location is not None:
                data['location'] = location

            output_path = os.path.join(output_dir, oerwm_id)
            with open(output_path, 'w') as file:
                json.dump(data, file)
            print "Wrote data for " + oerwm_id

def list_members():
    url = "http://members.ocwconsortium.org/api/v1/organization/group_by/membership_type/list/?format=json"
    response = urllib2.urlopen(url)
    return json.loads(response.read())

def get_details(member_id):
    url = "http://members.ocwconsortium.org/api/v1/organization/view/" + str(member_id) + "/?format=json"
    response = urllib2.urlopen(url)
    return json.loads(response.read())

def get_feature_collection():
    url = "http://members.ocwconsortium.org/api/v1/address/list/geo/?format=json"
    response = urllib2.urlopen(url)
    return json.loads(response.read())

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
    url = "http://nominatim.openstreetmap.org/reverse?format=json&lat=" + str(lat) + "&lon=" + str(lon) + "&addressdetails=1"
    req = urllib2.Request(url, None, headers)
    response = urllib2.urlopen(req)
    address = json.loads(response.read())['address']
    data = {
        '@type': 'Place',
        'geo': {
            'lat': lat,
            'lon': lon
        },
        'address': {}
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
