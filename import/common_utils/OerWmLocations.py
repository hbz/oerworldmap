import BeautifulSoup, urllib2, json, re, os, sys, urlparse, pycountry, datetime, base64, urllib


def analyze_location_with_mapzen(location, focus, mapzen_api_key):
    result = get_json_from_mapzen(location, focus, mapzen_api_key)
    if result and result['features']:
        first_hit = result['features'][0]
        return first_hit_to_json_schema(first_hit)
    return None


def get_json_from_mapzen(location, focus, mapzen_api_key):
    url = 'https://search.mapzen.com/v1/autocomplete?api_key=' + mapzen_api_key + '&text=' + location.encode('utf-8')
    if not focus is None:
        url = url + '&focus.point.lat=' + str(focus['lat'])
        url = url + '&focus.point.lon=' + str(focus['lon'])
    print 'url: ' + url
    response = urllib2.urlopen(url)
    return json.loads(response.read())


def first_hit_to_json_schema(json):
    properties = json['properties']
    address = {'streetAddress': properties['name']}
    if 'country_a' in properties:
        address['addressCountry'] = iso3166alpha3_to_iso3166alpha2(properties['country_a'])
    if 'postalcode' in properties:
        address['postalCode'] = properties['postalcode']
    if 'locality' in properties:
        address['addressLocality'] = properties['locality']
    return address


def iso3166alpha3_to_iso3166alpha2(iso3):
    for country in pycountry.countries:
        if country.alpha_3 == iso3:
            return country.alpha_2
    return iso3
