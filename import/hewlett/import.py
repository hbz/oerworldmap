import BeautifulSoup, urllib2, json, re, os, sys, uuid, urlparse, pycountry, geotext


grant_mapping = {
    'Amount': 'frapo:hasMonetaryValue',
    'Date Awarded': 'frapo:hasAwardDate'
}

url_page_regex = re.compile(r"page/([0-9]+)/")
grant_url_id_regex = re.compile(r"/grants/([0-9a-zA-Z-]+)/.*")
grantee_url_id_regex = re.compile(r"search_grantee=([0-9]+)$")
month_duration_regex = re.compile(r"([0-9]+) [Mm]onths")
address_without_span_regex = re.compile(r'(?<=(<div class="aboutgrantee-address">\n))(.*)(?=(</div>))')
address_split_regex = re.compile(r'<br ?/?>')
oer_regex = re.compile(r'([\W]*)(OER)([\W]*)')
suite_regex = re.compile(r'((Suite #?)|#)(\d*)')
street_regex = re.compile(r'(One|[\d]{1,5}).*([Aa]ve(nue)?|B(ou)?le?v(ar)?d\.?|Broadway|Building|Circle|Court|Dr(ive)?|H[ai]ll|Lane|Pa?r?kwa?y|Pl(a(ce|za))?|R(oad|d\.?)|(\bS|\ws)t((r(\.|eet|a(.|ss)e)?)|\.)?|[Ww](ay|eg)|(\bW|\ww)ood)(,? [NS]\.?[EW]\.?)?(?=(,|$| #))')
region_regex = re.compile(r'^([A-Z]{2})-[A-Z]{2,3}$')
pobox_regex = re.compile(r'((P\.?O\.? )?Box|Post(bus|fach)) ([\d]{2,6})')
postalcode_regex = re.compile(r'[\d]{5}(-[\d]{4})?|[A-Z][0-9][A-Z] [0-9][A-Z][0-9]|[A-Z]{1,2}[0-9]{1,2} [0-9][A-Z]{2}')

uuid_file = "id_map.json"
uuids = {}
address = {}
subdivisions_by_countries = {}


def fill_subdivision_list():
    global subdivisions_by_countries
    current_country = ''
    current_subdiv_list = []
    for subdivision in pycountry.subdivisions:
        if not subdivision.country_code.__eq__(current_country):
            subdivisions_by_countries[current_country] = current_subdiv_list
            current_country = str(subdivision.country_code)
            current_subdiv_list = []
        current_subdiv_list.append(str(subdivision.code))


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
            "xsd":"http://www.w3.org/2001/XMLSchema#",
            "ex": "http://example.org/"
        },
        "@type": "frapo:Grant"
    }


def get_uuid(key):
    global uuids
    try:
        a_uuid = uuids[key]
    except KeyError, e:
        # id does not yet exist
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


def save_ids_file():
    global uuids, uuid_file
    with open(uuid_file, 'w') as file:
        file.write(json.dumps(uuids))


def get_grant_id(url):
    parsed_url = urlparse.urlparse(url)
    if parsed_url[2]:
        match = re.search(grant_url_id_regex, parsed_url[2])
        if match.group(1):
            return match.group(1)
    return 0


def get_grantee_url(beautifulsoup):
    grantee_urls = beautifulsoup.findAll('div', {'class' : 'aboutgrantee-actions'})
    for grantee_url in grantee_urls:
        anchors = grantee_url.findChildren()
        for anchor in anchors:
            return "http://www.hewlett.org" + str(anchor['href'])


def get_grantee_id(url):
    parsed_url = urlparse.urlparse(url)
    if parsed_url[4]:
        match = re.search(grantee_url_id_regex, parsed_url[4])
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


def is_desired(strategies, highlight_lis):
    has_strategy = False
    has_support_type = False
    for strategy in strategies:
        if strategy.getText() == 'OER':
            has_strategy = True
    for highlight_li in highlight_lis:
        for highlight_label in highlight_li.findAll('div', {'class' : 'highlights-label'}):
            if highlight_label.getText() == 'Type of Support':
                for highlight_value in highlight_li.findAll('div', {'class' : 'highlights-value'}):
                    if highlight_value.getText() == 'Project':
                        has_support_type = True
    return has_strategy and has_support_type


def fill_address(address_line):
    global address
    address_line = strip_line(address_line)
    if address.get('schema:streetAddress', None) == None:
        address_line = fill_street(address_line)
    if address.get('schema:countryAddress', None) == None:
        address_line = fill_country(address_line)
    if address.get('schema:countryRegion', None) == None:
        address_line = fill_region(address_line)
    if address.get('schema:postOfficeBoxNumber', None) == None:
        address_line = fill_pobox(address_line)
    if address.get('schema:postalCode', None) == None:
        address_line = fill_postalcode(address_line)

def fill_street(address_line):
    global address
    # extract suite
    suite = None
    match = re.search(suite_regex, address_line)
    if match :
        suite = match.group(3)
    address_line = re.sub(suite_regex, ', ', address_line)
    # extract streetAddress
    street = None
    match = re.search(street_regex, address_line)
    if match :
        street = match.group(0)
    address_line = re.sub(street_regex, ', ', address_line)
    if suite and street:
        street = street + ", Suite " + suite
    address['schema:streetAddress'] = street
    return strip_line(address_line)


def fill_country(address_line):
    global address
    subword = None
    words = re.split(r'[ ,.\-_]', address_line)
    for word in words:
        length = len(word)
        if length < 2:
            continue
        if length == 2:
            for country in pycountry.countries:
                if country.alpha_2 == word:
                    address['schema:addressCountry'] = subword = word
        else:
            if length == 3:
                for country in pycountry.countries:
                    if country.alpha_3 == word:
                        subword = word
                        address['schema:addressCountry'] = str(country.alpha_2)
            else:
                for country in pycountry.countries:
                    if country.name.encode('utf-8').strip().__eq__(word):
                        subword = word
                        address['schema:addressCountry'] = str(country.alpha_2)
    if subword:
        address_line = address_line.replace(subword, '')
    return strip_line(address_line)


def fill_region(address_line):
    global address
    country = str(address.get('schema:addressCountry'))
    country_subdivisions = None
    if country:
        country_subdivisions = subdivisions_by_countries.get(country)
    words = re.split(r'[ ,._]', address_line)
    for word in words:
        if len(word) < 2:
            continue
        if len(word) == 2:
            if country_subdivisions:
                for csv in country_subdivisions:
                    if word.__eq__(csv.replace(country + '-', '')):
                        address['schema:addressRegion'] = csv
                        address_line = address_line.replace(csv, '')
                        return strip_line(address_line)
        match = re.search(region_regex, word)
        if match:
            if not country:
                country = match.group(1)
            subdivisions = subdivisions_by_countries.get(country)
            if subdivisions:
                for subdivision in subdivisions:
                    if word.__eq__(subdivision):
                        address['schema:addressRegion'] = word
                        address['schema:addressCountry'] = country
                        address_line = address_line.replace(word, '')
                        return strip_line(address_line)
        # TODO: solution for fully worded regions?
    return address_line


def fill_pobox(address_line):
    global address
    match = re.search(pobox_regex, address_line)
    if match:
        address['schema:postOfficeBoxNumber'] = match.group(4)
        address_line = address_line.replace(match.group(0), '')
        return strip_line(address_line)
    return address_line


def fill_postalcode(address_line):
    # NOTE: for the current implementation, it is important to run fill_pobox before fill_postalcode.
    # Otherwise, pobox information will be matched as postalcode.
    global address
    match = re.search(postalcode_regex, address_line)
    if match:
        address['schema:postalCode'] = match.group(0)
        address_line = address_line.replace(match.group(0), '')
        return strip_line(address_line)
    return address_line


def strip_line(line):
    line = re.sub(r', ?,', ',', line)
    line = re.sub(r', ?,', ',', line)
    line = line.strip()
    line = re.sub(r',$', '', line)
    return line


def collect(url):
    global address
    awarder = {
        "@id":"urn:uuid:0801e4d4-3c7e-11e5-9f0e-54ee7558c81f"
    }
    action = {
        "@type":"schema:Action"
    }
    agent = {
        "@type":"schema:Organization"
    }
    location = {
        "@type":"schema:Place"
    }
    address = {
        "@type":"schema:PostalAddress"
    }

    soup = get_soup_from_page(url)
    if not soup:
        return None
    highlight_lis = soup.findAll('li', {'class' : 'highlight'})
    strategies = soup.findAll('li', {'class' : 'highlight-strategy'})
    if not is_desired(strategies, highlight_lis):
        return None

    grant_id = get_grant_id(url)
    grantee_url = get_grantee_url(soup)
    grantee_id = get_grantee_id(grantee_url)
    result = get_header()
    result['@id'] = get_uuid('hewlett_grant_' + grant_id)
    action['@id'] = get_uuid('hewlett_action_' + grant_id)
    agent['ex:granteeNumber'] = grantee_id
    agent['ex:hewlettGrantList'] = grantee_url

    if hasattr(soup, 'h1'):
        agent['schema:name'] = {
            "@language": "en",
            "@value": soup.find('h1').getText()
        }
        print(soup.find('h1').getText()) # for status control
    if hasattr(soup, 'h3'):
        action['schema:name'] = {
            "@language": "en",
            "@value": soup.find('h3').getText()
        }
    address_tags = soup.findAll('div', { "class" : "aboutgrantee-address" })
    for address_tag in address_tags:
        address_no_span = re.search(address_without_span_regex, (str(address_tag)))
        address_split = re.split(address_split_regex, address_no_span.group(0))
        # TODO: addresses are formatted even more heterogeneous in the current hewlett page design
        # TODO: ==> find 'intelligent' rules to match street vs. locality vs. country
        for splitpart in address_split:
            fill_address(splitpart)
    # print('address: ' + `address`)
    overviews = soup.findAll('div', { "class" : "grant-overview" })
    for overview in overviews:
        result['schema:description'] = {
            "@language":"en",
            "@value":overview.getText()
        }
    subtitles = soup.findAll('h3', { "class" : "large-subtitle" })
    for subtitle in subtitles:
        result['schema:name'] = {
            "@language":"en",
            "@value":subtitle.getText()
        }
    for highlight_li in highlight_lis:
        label = None
        value = None
        for highlight_label in highlight_li.findAll('div', {'class' : 'highlights-label'}):
            label = highlight_label.getText()
        for highlight_value in highlight_li.findAll('div', {'class' : 'highlights-value'}):
            value = highlight_value.getText()
        if label in grant_mapping:
            result[grant_mapping[label]] = value
        elif label.__eq__('Term'):
            duration = get_grant_duration(value)
            result['schema:duration'] = duration
            action['schema:duration'] = duration
    location['schema:address'] = address
    agent['schema:location'] = location
    action['schema:agent'] = agent
    result['frapo:funds'] = action
    result['frapo:is_awarded_by'] = awarder
    return json.dumps(result, indent=2)


def crawl_page(url):
    links = []
    soup = get_soup_from_page(url)
    if soup:
        for anchor in soup.findAll('a', {'class' : 'listing-highlight-link'}):
            link = anchor.get('href')
            links.append(link)
    return links


def next_page(url, count):
    url_split = re.split(re.compile(r"[?]"), url)
    page = url_split[0]
    page += 'page/' + str(count) + '/?' + url_split[1]
    print('page: ' + `page`) # for status control
    return crawl_page(page)


def get_page_number(url):
    match = re.search(url_page_regex, url)
    if match:
        if match.group(1):
            return int(match.group(1))
    return 1


def get_url_without_page(url):
    match = re.sub(url_page_regex, '', url)
    if not match:
        return url
    return match


def import_hewlett_data(url):
    imports = []
    current_number = get_page_number(url)
    url_no_page = get_url_without_page(url)
    while 1:
        links = next_page(url_no_page, current_number)
        if links.__eq__([]):
            break
        for link in links:
            json = collect(link)
            if json != None:
                imports.append(json)
        current_number += 1
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
        # python import/hewlett/import.py 'http://www.hewlett.org/grants/?search=oer' 'import/hewlett/search_oer.json'
        # python import/hewlett/import.py 'http://www.hewlett.org/grants/?search=open+educational+resources' 'import/hewlett/search_open_educational_resources.json'
        return
    load_ids_file()
    fill_subdivision_list()
    imports = import_hewlett_data(sys.argv[1])
    print('Hewlett import: found ' + `len(imports)` + ' items.')
    write_into_file(imports, sys.argv[2])
    save_ids_file()


if __name__ == "__main__":
    main()
