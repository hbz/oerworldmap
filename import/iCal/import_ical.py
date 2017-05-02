import BeautifulSoup, urllib2, json, re, os, sys, uuid, urlparse, pycountry, datetime, base64, urllib, StringIO


path = os.path.dirname(os.path.realpath(__file__)) + os.path.sep
uuid_file = path + "id_map.json"
uuids = {}
import_list = []
mapzen_api_key = None


def read_content_from_url(url):
    try:
        site = urllib2.urlopen(url)
        encoding=site.headers['content-type'].split('charset=')[-1]
        content = site.read()
    except urllib2.URLError, e:
        if hasattr(e, 'reason'):
            print 'We failed to reach a server.'
            print 'Reason: ', e.reason
        elif hasattr(e, 'code'):
            print 'The server couldn\'t fulfill the request.'
            print 'Error code: ', e.code
        return ""
    if encoding.__eq__('text/plain'):
        return content
    return unicode(content, encoding)


def load_ids_from_file(id_file, ids):
    if not os.path.isfile(id_file):
        open(id_file, 'a').close()
    try:
        with open(id_file, 'r') as file:
            ids = json.loads(file.read())
    except ValueError, e:
        if not str(e).__eq__('No JSON object could be decoded'):
            raise ValueError('Unexpected error while loading ids file: ' + str(e))
    return ids


def save_ids_to_file(ids, id_file):
    with open(id_file, 'w') as file:
        file.write(json.dumps(ids))


def write_list_into_file(import_list, filename):
    output_file = open(filename, "w")
    count = 1
    output_file.write("[")
    for import_entry in import_list:
        output_file.write(import_entry)
        if count < len(import_entry):
            output_file.write(",\n")
            count += 1
    output_file.write("]")
    output_file.close()


def read_content_from_file(file, encoding):
    if file.startswith('file://'):
        file = file[7:]
    f = codecs.open(file, 'r', encoding)
    return f.read()


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
    time.sleep(1.0)
    response = urllib2.urlopen(url)
    print 'response: ' + `response`
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


def readline(buffer):
    line = buffer.readline()
    if line.endswith('\n'):
        line = line[:-1]
    if line.endswith('\r'):
        line = line[:-1]
    return line


def read_until(buffer, delimiter_line):
    result = []
    line = readline(buffer)
    while line:
        if line.__eq__(delimiter_line):
            break
        else:
            result.append(line)
            line = readline(buffer)
    return result


def read_header(buffer):
    return read_until(buffer, "BEGIN:VEVENT")


def read_next_event(buffer):
    return read_until(buffer, "END:VEVENT")


def split_address(address_string, focus):
    if address_string.startswith('LOCATION:'):
        address_string = address_string[9:].strip()
    if address_string.__eq__(''):
        return None
    return analyze_location_with_mapzen(address_string, focus, mapzen_api_key)


def format_date(date_string):
    # TODO: For now, this function always returns a date time as UTC formatted. Implement time zone reference.
    if date_string.startswith('VALUE=DATE:'):
        date_string = date_string[11:]
    if not date_string.endswith('Z'):
        date_string = date_string + 'Z'
    match = re.search(re.compile(r"([0-9]{4})([0-9]{2})([0-9]{2})T([0-9]{2})([0-9]{2})([0-9]{2})Z"), date_string)
    if match:
        return str(match.group(1) + '-' + match.group(2) + '-' + match.group(3) + 'T' + match.group(4) + ':' + match.group(5) + ':' + match.group(6) + 'Z')
    match = re.search(re.compile(r"([0-9]{4})([0-9]{2})([0-9]{2})Z"), date_string)
    if match:
        return str(match.group(1) + '-' + match.group(2) + '-' + match.group(3) + 'T00:00:00Z:')
    return date_string


def lines_to_resource(header, event, language):
    resource = {'@type': 'Event', '@context': 'https://oerworldmap.org/assets/json/context.json'}
    location = {}
    for line in event:
        if line.startswith("SUMMARY:"):
            name = {'@value': line[8:], '@language': language}
            resource['name'] = [name]
        elif line.startswith("DESCRIPTION"):
            description = {'@value': line[12:], '@language': language}
            resource['description'] = [description]
        elif line.startswith("DTSTART"):
            resource['startDate'] = format_date(line[8:])
        elif line.startswith("DTEND"):
            resource['endDate'] = format_date(line[6:])
        elif line.startswith("CREATED"):
            resource['dateCreated'] = format_date(line[8:])
        elif line.startswith("GEO:"):
            coordinates = line[4:].split(";")
            geo = {'lat': coordinates[0], 'lon': coordinates[1]}
            location['geo'] = geo
        elif line.startswith("LOCATION:"):
            if 'geo' in location:
                line = split_address(line, location['geo'])
            else:
                line = split_address(line, None)
            if not line is None:
                location['address'] = line
        elif line.startswith("ORGANIZER"):
            line = line[10:]
            match = re.search(".*CN=([\w ,.]*).*", line)
            if match:
                name = {'@value': match.group(1), '@language': language}
                organizer = {'name': [name]}
                resource['organizer'] = organizer
                # TODO: determine '@type' and '@id' of organizer
            line = line.lower()
            match = re.search(".*(?<!(sent-by=\"))mailto:([\w@.]*).*", line)
            if match:
                resource['email'] = match.group(2)
            # subfields SENT-BY and DIR are ignored here
        elif line.startswith("URL:"):
            resource['url'] = line[4:]
        if location:
            resource['location'] = location
    resource['@id'] = uuid.uuid4().urn
    return resource


def import_ical_from_string(page_content, language):
    imports = []
    buffer = StringIO.StringIO(page_content)
    header = read_header(buffer)
    event = read_next_event(buffer)
    while event:
        resource = lines_to_resource(header, event, language)
        imports.append(resource)
        line = readline(buffer)
        if line.__eq__("END:VCALENDAR"):
            break
        else:
            event = read_next_event(buffer)
    return json.dumps(imports, indent=2)


def import_ical_from_url(url, language):
    # this function expects a url purely providing a or several iCal event(s)
    if url.startswith('file://'):
        page_content = read_content_from_file(url, 'utf-8')
        # TODO: do we need file encoding as a parameter?
    else:
        page_content = read_content_from_url(url)
    if not language:
        language = 'en'
    imports = import_ical_from_string(page_content, language)
    return imports


def import_ical(url, language, api_key):
    global path, uuids, import_list, mapzen_api_key
    mapzen_api_key = api_key
    load_ids_from_file(path + "id_map.json", uuids)
    import_list = import_ical_from_url(url, language)
    save_ids_to_file(uuids, path + "id_map.json")
    return import_list


def main():
    if len(sys.argv) != 6:
        print 'Usage: python -m import.iCal.import_ical <path>/<to>/import_ical.py <import_url> <language> <path>/<to>/<destination_file.json> <mapzen-api-key>'
        print 'Please provide the iCal event language as ISO 3166 ALPHA 2 country code.'
        return
    return import_ical(sys.argv[2], sys.argv[3], sys.argv[5])


if __name__ == "__main__":
    import_list = main()
    write_list_into_file(import_list, sys.argv[4])
    # print "Events: " + `import_list`
