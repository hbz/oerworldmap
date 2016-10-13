import BeautifulSoup, urllib2, json, re, os, sys, uuid, urlparse


grant_mapping = {
    'Amount:': 'frapo:hasMonetaryValue',
    'Date of Award:': 'frapo:hasAwardDate'
}


page_regex = re.compile(r"&page=([0-9]+)$")
url_without_page_regex = re.compile(r"(.+)(?=(&page=[0-9]+))")
url_term_regex = re.compile(r"/grants/([0-9a-zA-Z]+)/.+")
month_duration_regex = re.compile(r"([0-9]+) [Mm]onths")
address_without_span_regex = re.compile(r'(?<=(<span class="grant-address">))(.*)(?=(</span>))')
address_split_regex = re.compile(r'<br ?/?>')
uuid_file = "id_map.json"
uuids = {}


def get_soup_from_page(url):
    try:
        page = urllib2.urlopen(url).read()
    except urllib2.URLError, e:
        if hasattr(e, 'reason'):
            print 'We failed to reach a server.'
            print 'Reason: ', e.reason
        elif hasattr(e, 'code'):
            print 'The server couldn\'t fulfill the request.'
            print 'Error code: ', e.code
    else:
        soup = BeautifulSoup.BeautifulSoup(page)
        soup.prettify()
        return soup


def get_header():
    return {
        "@context":{
            "frapo":"http://purl.org/cerif/frapo/",
            "rdf":"http://www.w3.org/1999/02/22-rdf-syntax-ns#",
            "rdfs":"http://www.w3.org/2000/01/rdf-schema#",
            "schema":"http://schema.org/",
            "xsd":"http://www.w3.org/2001/XMLSchema#"
        },
        "@type": "frapo:Grant"
    }


def get_uuid(key):
    global uuids
    a_uuid = uuids[key]
    if a_uuid is None:
        a_uuid = uuid.uuid4().urn
        put_uuid(key, a_uuid)
    return a_uuid


def put_uuid(key, uuid):
    global uuids
    uuids[key] = uuid


def load_ids_file():
    global uuids, uuid_file
    if not os.path.isfile(uuid_file):
        open(uuid_file, 'a').close()
    try:
        with open(uuid_file, 'r') as file:
            uuids = json.loads(file.read())
    except ValueError, e:
        if not str(e).__eq__('No JSON object could be decoded'):
            raise ValueError('Unexpected error while loading ids file: ' + str(e))
    print "load uuids: " + str(uuids) # TODO delete


def save_ids_file():
    global uuids, uuid_file
    with open(uuid_file, 'w') as file:
        file.write(json.dumps(uuids))
    print "save uuids: " + str(uuids) # TODO delete


def get_grant_number(url):
    parsed_url = urlparse.urlparse(url)
    if parsed_url[2]:
        match = re.search(url_term_regex, parsed_url[2])
        if match.group(1):
            return match.group(1)
    return 0


def get_grant_duration(value):
    match = re.search(month_duration_regex, value)
    if match.group(1):
        return 'P' + match.group(1) + 'M'
    else:
        raise ValueError('Unknown duration format:' + value)
        return value


def collect(url):
    awarder = {
        "@id":"urn:uuid:0801e4d4-3c7e-11e5-9f0e-54ee7558c81f"
    }
    action = {
        "@type":"schema:Action"
    }
    agent = {}
    location = {
        "@type":"schema:Place"
    }
    address = {
        "@type":"schema:PostalAddress"
    }

    result = get_header()
    grant_number = get_grant_number(url)
    result['frapo:hasGrantNumber'] = grant_number
    result['@id'] = get_uuid('hewlett_grant_' + grant_number)
    soup = get_soup_from_page(url)

    if hasattr(soup, 'h3'):
        agent['schema:name'] = {
            "@language": "en",
            "@value": soup.find('h3').getText()
        }
    address_tags = soup.findAll('span', { "class" : "grant-address" })
    for address_tag in address_tags:
        address_no_span = re.search(address_without_span_regex, (str(address_tag)))
        address_split = re.split(address_split_regex, address_no_span.group(0))
        if address_split[0]:
            address['schema:streetAddress'] = address_split[0]
        if address_split[1]:
            address['schema:addressLocality'] = address_split[1]
        if address_split[2]:
            address['schema:addressCountry'] = address_split[2]
    if hasattr(soup, 'tbody'):
        for table in soup.findAll('tbody'):
            for row in table.findAll('tr'):
                entry = row.findAll('td')
                field = entry[0].getText()
                if field in grant_mapping:
                    result[grant_mapping[field]] = entry[1].getText()
                elif field.__eq__('Grant Description:'):
                    result['schema:description'] = {
                        "@language":"en",
                        "@value":entry[1].getText()
                    }
                elif field.__eq__('Term of Grant:'):
                    duration = get_grant_duration(entry[1].getText())
                    result['schema:duration'] = duration
                    action['schema:duration'] = duration
                elif field.__eq__('Grant Purpose:'):
                    action['schema:name'] = {
                        "@language": "en",
                        "@value": entry[1].getText()
                    }
    location['schema:address'] = address
    agent['schema:location'] = location
    action['schema:agent'] = agent
    result['frapo:funds'] = action
    result['frapo:is_awarded_by'] = awarder
    return json.dumps(result, indent=2)


def crawl_page(url):
    links = []
    soup = get_soup_from_page(url)
    for table in soup.findAll('tbody'):
        for row in table.findAll('tr'):
            cols = row.findAll('td')
            link = cols[0].find('a').get('href')
            links.append('http://www.hewlett.org' + link)
    return links


def next_page(url, count):
    page = url
    page += '&page=' + str(count)
    return crawl_page(page)


def get_page_number(url):
    match = re.search(page_regex, url)
    if match.group(1):
        return int(match.group(1))
    return 0


def get_url_without_page(url):
    match = re.search(url_without_page_regex, url)
    if match.group(1):
        return match.group(1)
    return url


def import_hewlett_data(url):
    imports = []
    # current_number = get_page_number(url) TODO
    current_number = 0 # TODO delete
    url_no_page = get_url_without_page(url)
    # while 1: #TODO
    links = next_page(url_no_page, current_number)
    # if links.__eq__([]):  TODO
    #     break             TODO
    for link in links:
        json = collect(link)
        imports.append(json)
    current_number += 1
    # TODO: while end
    return imports


def write_into_file(imports, filename):
    output_file = open(filename, "w")
    count = 1;
    output_file.write("[")
    for import_entry in imports:
        output_file.write(import_entry)
        if count < len(imports):
            output_file.write(",\n")
            count += 1
    output_file.write("]")
    output_file.close()


def main():
    if len(sys.argv) != 3:
        print 'Usage: python <path>/<to>/import.py <import_url> <path>/<to>/<destination_file.json>'
        # typical usage:
        # python import/hewlett/import.py 'http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148' 'import/hewlett/testconsole_01.json'
        return
    load_ids_file()
    imports = import_hewlett_data(sys.argv[1])
    write_into_file(imports, sys.argv[2])
    save_ids_file()

if __name__ == "__main__":
    main()
