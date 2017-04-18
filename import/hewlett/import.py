import BeautifulSoup, urllib2, json, re, os, sys, uuid, urlparse, pycountry, datetime, base64, urllib


grant_mapping = {
    'Amount': 'hasMonetaryValue'
}

url_page_regex = re.compile(r"page/([0-9]+)/")
grant_url_id_regex = re.compile(r"/grants/([0-9a-zA-Z-]+)/.*")
grantee_url_id_regex = re.compile(r"search_grantee=([0-9]+)$")
month_duration_regex = re.compile(r"([0-9]+) [Mm]onths")
address_without_span_regex = re.compile(r'(?<=(<div class="aboutgrantee-address">\n))(.*)(?=(</div>))')
address_split_regex = re.compile(r'<br ?/?>')
oer_regex = re.compile(r'([\W]*)(OER)([\W]*)')
suite_regex = re.compile(r'((Suite #?)|#)(\d*)')
room_regex = re.compile(r'R(oom|m\.?) (\d*)')
floor_regex = re.compile(r'((1|Fir)st|(2|Seco)nd|(3|Thi)rd|([\d]|Four|Fif|Six|Seven|Eight|Nine|Ten|Eleven|Twelv){1,3}(th)?) Floor')
block_regex = re.compile(r'[A-Z] Block')
street_regex_1 = re.compile(r'(One|[\d]{1,5}).*([Aa]ve(nue)?|B(ou)?le?v(ar)?d\.?|Broadway|Building|Circle|Court|Dr(ive)?|H[ai]ll|Lane|Pa?r?kwa?y|Pl(a(ce|za))?|R(oad|d\.?|u[ea])|(\bS|\ws)t((r(\.|eet|a(.|ss)e)?)|\.)?|[Ww](ay|eg)|(\bW|\ww)ood)(,? [NS]\.?[EW]\.?)?(?=(,|$| #))')
street_regex_2 = re.compile(r'[\d]{1,5},? ([Aa]venue|[Rr]u[ae]|[Pp]lace)( de)?( [^\d#,]*)')
region_regex_1 = re.compile(r'([A-Z]{2})-([A-Z]{2,3})')
region_regex_2 = re.compile(r'(?<=( ))[A-Z]{2}(?=( |$|,))')
country_regex_1 = re.compile(r'(?<=( ))([A-Z]{2,3})(?=( |$|,))')
country_regex_2 = re.compile(r'Afghanistan|Albania|Algeria|Andorra|Angola|Antigua and Barbuda|Argentina|Armenia|Australia|Austria|Azerbaijan|Bahamas|Bahrain|Bangladesh|Barbados|Belarus|Belgium|Belize|Benin|Bhutanh|Bolivia|Bosnia and Herzegovina|Botswana|Brazil|Brunei|Bulgaria|Burkina Faso|Burundi|Cabo Verde|Cambodia|Cameroon|Canada|Central African Republic (CAR)|Chad|Chile|China|Colombia|Comoros|Democratic Republic of the Congo|Republic of the Congo|Costa Rica|Cote d.Ivoire|Croatia|Cuba|Cyprus|Czech Republic|Denmark|Djibouti|Dominica|Dominican Republic|Ecuador|Egypt|El Salvador|Equatorial Guinea|Eritrea|Estonia|Ethiopia|Fiji|Finland|France|Gabon|Gambia|Georgia|Germany|Ghana|Greece|Grenada|Guatemala|Guinea|Guinea-Bissau|Guyana|Haiti|Honduras|Hungary|Iceland|India|Indonesia|Iran|Iraq|Ireland|Israel|Italy|Jamaica|Japan|Jordan|Kazakhstan|Kenya|Kiribati|Kosovo|Kuwait|Kyrgyzstan|Laos|Latvia|Lebanon|Lesotho|Liberia|Libya|Liechtenstein|Lithuania|Luxembourg|Macedonia|Madagascar|Malawi|Malaysia|Maldives|Mali|Malta|Marshall Islands|Mauritania|Mauritius|Mexico|Micronesia|Moldova|Monaco|Mongolia|Montenegro|Morocco|Mozambique|Myanmar (Burma)|Namibia|Nauru|Nepal|Netherlands|New Zealand|Nicaragua|Niger|Nigeria|North Korea|Norway|Oman|Pakistan|Palau|Palestine|Panama|Papua New Guinea|Paraguay|Peru|Philippines|Poland|Portugal|Qatar|Romania|Russia|Rwanda|Saint Kitts and Nevis|Saint Lucia|Saint Vincent and the Grenadines|Samoa|San Marino|Sao Tome and Principe|Saudi Arabia|Senegal|Serbia|Seychelles|Sierra Leone|Singapore|Slovakia|Slovenia|Solomon Islands|Somalia|South Africa|South Korea|South Sudan|Spain|Sri Lanka|Sudan|Suriname|Swaziland|Sweden|Switzerland|Syria|Taiwan|Tajikistan|Tanzania|Thailand|Timor-Leste|Togo|Tonga|Trinidad and Tobago|Tunisia|Turkey|Turkmenistan|Tuvalu|Uganda|Ukraine|United Arab Emirates|United Kingdom|United States of America|Uruguay|Uzbekistan|Vanuatu|Vatican City|Venezuela|Vietnam|Yemen|Zambia|Zimbabwe')
pobox_regex = re.compile(r'((P\.?O\.? )?Box|Post(bus|fach)) ([\d]{2,6})')
postalcode_regex = re.compile(r'[\d]{5}(-[\d]{4})?|[A-Z][0-9][A-Z] [0-9][A-Z][0-9]|[A-Z]{1,2}[0-9]{1,2} [0-9][A-Z]{2}')

path = os.path.dirname(os.path.realpath(__file__)) + os.path.sep
uuid_file = path + "id_map.json"
agents_file = path + "agents.json"
cache_dir = path + "cache" + os.path.sep
agents = []
uuids = {}
new_uuids = []
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
    if not is_cached(url):
        to_cache(url)
    soup = BeautifulSoup.BeautifulSoup(from_cache(url))
    soup.prettify()
    return soup


def to_cache(url):
    print "To cache: " + url
    try:
        f = open(get_filename(url), 'w')
        f.write(urllib2.urlopen(url).read())
        f.close()
    except urllib2.URLError, e:
        if hasattr(e, 'reason'):
            print 'We failed to reach a server.'
            print 'Reason: ', e.reason
        elif hasattr(e, 'code'):
            print 'The server couldn\'t fulfill the request.'
            print 'Error code: ', e.code


def from_cache(url):
    print "From cache: " + url
    f = open(get_filename(url), 'r')
    page = f.read()
    f.close()
    return page


def is_cached(url):
    return os.path.exists(get_filename(url))


def get_filename(url):
    return cache_dir + urllib.quote_plus(url)


def get_and_put_uuid(key):
    uuid = get_uuid(key)
    if not uuid:
        uuid = create_uuid()
        put_uuid(key, uuid)
    return uuid


def get_uuid(key):
    global uuids
    try:
        a_uuid = uuids[key]
    except KeyError, e:
        return None
    return a_uuid


def create_uuid():
    return uuid.uuid4().urn


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


def load_agents_file():
    global agents, agents_file
    if not os.path.isfile(agents_file):
        open(agents_file, 'a').close()
    try:
        with open(agents_file, 'r') as file:
            agents = json.loads(file.read())
    except ValueError, e:
        if not str(e).__eq__('No JSON object could be decoded'):
            raise ValueError('Unexpected error while loading agents file: ' + str(e))


def save_agents_file():
    global agents, agents_file
    with open(agents_file, 'w') as file:
        file.write(json.dumps(agents))


def get_agent_by_id(search_id):
    global agents
    for agent in agents:
        id = agent.get('@id')
        if id and id == search_id:
            return agent
    return None


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
            return "http://www.hewlett.org" + str(anchor['href']).strip()


def get_grantee_external_url(beautifulsoup):
    grantee_urls = beautifulsoup.findAll('a', {'class' : 'aboutgrantee-extra-value is-lowercase'})
    for grantee_url in grantee_urls:
        return grantee_url['href'].strip()


def get_grantee_id(url):
    parsed_url = urlparse.urlparse(url)
    if parsed_url[4]:
        match = re.search(grantee_url_id_regex, parsed_url[4])
        if match.group(1):
            return match.group(1)
    return 0


def get_grant_date(value):
    date = value.split("/")
    return datetime.date(int(date[2]), int(date[0]), int(date[1])).isoformat()


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


def extract_address(address_line):
    global address
    address_line = strip_line(address_line)
    address_line = split_camel(address_line)
    address_line = extract_street(address_line)
    address_line = extract_region(address_line)
    address_line = extract_country(address_line)
    address_line = extract_pobox(address_line)
    address_line = extract_postalcode(address_line)
    extract_locality(address_line)
    # print (address) # for extraction control


def extract_street(address_line):
    global address
    # extract suite
    suite = None
    match = re.search(suite_regex, address_line)
    if match :
        suite = match.group(3)
        address_line = strip_line(re.sub(match.group(0), ', ', address_line))
    # extract floor
    floor = None
    match = re.search(floor_regex, address_line)
    if match :
        floor = match.group(0)
        address_line = strip_line(re.sub(floor, ', ', address_line))
    # extract room
    room = None
    match = re.search(room_regex, address_line)
    if match :
        room = match.group(2)
        address_line = strip_line(re.sub(match.group(0), ', ', address_line))
    # extract block
    block = None
    match = re.search(block_regex, address_line)
    if match :
        block = match.group(0)
        address_line = strip_line(re.sub(block, ', ', address_line))
    # extract streetAddress
    street = None
    match = re.search(street_regex_1, address_line)
    if not match:
        match = re.search(street_regex_2, address_line)
    if match :
        street = strip_line(match.group(0))
    if street:
        address_line = re.sub(street, ', ', address_line)
        if room:
            street = street + ", Room " + room
        if suite:
            street = street + ", Suite " + suite
        if floor:
            street = street + ", " + floor
        if block:
            street = street + ", " + block
        address['streetAddress'] = street
        address_line = strip_line(address_line)
    return address_line


def extract_country(address_line):
    global address
    match = re.search(country_regex_1, address_line)
    if match:
        if len(match.group(2)) == 2:
            for country in pycountry.countries:
                if country.alpha_2 == match.group(2) and not address.get('addressCountry'):
                    address['addressCountry'] = match.group(2)
                    address_line = address_line.replace(match.group(2), '')
                    return strip_line(address_line)
        else:
            if len(match.group(2)) == 3:
                for country in pycountry.countries:
                    if country.alpha_3 == match.group(2) and not address.get('addressCountry'):
                        address['addressCountry'] = match.group(2)
                        address_line = address_line.replace(match.group(2), '')
                        return strip_line(address_line)
    for country in pycountry.countries:
        match = re.search(country_regex_2, address_line)
        if match:
            if country.name.encode('utf-8').strip().__eq__(match.group(0)):
                address['addressCountry'] = str(country.alpha_2)
                address_line = address_line.replace(match.group(0), '')
    return strip_line(address_line)


def extract_region(address_line):
    global address
    match = re.search(region_regex_1, address_line)
    if match:
        subdivisions = subdivisions_by_countries.get(match.group(1))
        if subdivisions:
            for subdivision in subdivisions:
                if match.group(2).__eq__(subdivision):
                    address['addressRegion'] = match.group(2)
                    address['addressCountry'] = match.group(1)
                    address_line = address_line.replace(match.group(0), '')
                    return strip_line(address_line)
    match = re.search(region_regex_2, address_line)
    if match:
        subdivisions = subdivisions_by_countries.get('US')
        for subdivision in subdivisions:
            if match.group(0).__eq__(re.search(r'(?<=-).*', subdivision)):
                address['addressRegion'] = match.group(0)
                address['addressCountry'] = 'US'
                address_line = address_line.replace(match.group(0), '')
                return strip_line(address_line)
    # TODO: solution for fully worded regions?
    return address_line


def extract_pobox(address_line):
    global address
    match = re.search(pobox_regex, address_line)
    if match:
        address['postOfficeBoxNumber'] = match.group(4)
        address_line = address_line.replace(match.group(0), '')
        return strip_line(address_line)
    return address_line


def extract_postalcode(address_line):
    # NOTE: for the current implementation, it is important to run extract_pobox before extract_postalcode.
    # Otherwise, pobox information will be matched as postalcode.
    global address
    match = re.search(postalcode_regex, address_line)
    if match:
        address['postalCode'] = match.group(0)
        address_line = address_line.replace(match.group(0), '')
        return strip_line(address_line)
    return address_line


def extract_locality(address_line):
    global address
    address['addressLocality'] = strip_line(address_line)


def strip_line(line):
    line = re.sub(r'^, ?', '', line)
    line = re.sub(r'^, ?', '', line)
    line = re.sub(r', ?,', ',', line)
    line = re.sub(r', ?,', ',', line)
    line = line.strip()
    match = re.search(r' {2,}(?=[\w])', line)
    while match and match.group(0):
        line = re.sub(match.group(0), ' ', line)
        match = re.search(r' {2,}(?=[\w])', line)
    line = re.sub(r',$', '', line)
    return line


def split_camel(line):
    while (1):
        match = re.match(r'(.*[a-z])([A-Z].*)', line)
        if not match:
            break
        line = match.group(1) + ', ' + match.group(2)
    return line


def collect(url):
    global address, agents, new_uuids
    action = {
        "@context" : "https://oerworldmap.org/assets/json/context.json",
        "@type" : "Action"
    }
    grant = {
        "@type": "Grant"
    }
    awarder = {
        "@id":"urn:uuid:0801e4d4-3c7e-11e5-9f0e-54ee7558c81f"
    }
    agent = {}
    location = {
        "@type":"Place"
    }
    address = {
        "@type":"PostalAddress"
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

    action_id = get_and_put_uuid('hewlett_action_' + grant_id)
    if action_id in new_uuids:
        return None
    action['@id'] = action_id
    new_uuids.append(action_id)

    agent_uuid = get_and_put_uuid('hewlett_grantee_' + grantee_id)
    if not agent_uuid:
        agent_uuid = create_uuid();
    if not get_agent_by_id(agent_uuid):
        put_agent(agent_uuid, grantee_id, soup, location)
    agent['@id'] = agent_uuid
    action['agent'] = [agent]

    if hasattr(soup, 'h1'):
        print(soup.find('h1').getText()) # for status control
    if hasattr(soup, 'h3'):
        action['name'] = [{
            "@language": "en",
            "@value": (re.sub(r'^For ', '', soup.find('h3').getText())).title()
        }]
    overviews = soup.findAll('div', { "class" : "grant-overview" })
    for overview in overviews:
        action['description'] = [{
            "@language":"en",
            "@value":overview.getText()
        }]
    duration = None
    awarddate = None
    for highlight_li in highlight_lis:
        label = None
        value = None
        for highlight_label in highlight_li.findAll('div', {'class' : 'highlights-label'}):
            label = highlight_label.getText()
        for highlight_value in highlight_li.findAll('div', {'class' : 'highlights-value'}):
            value = highlight_value.getText()
        if label in grant_mapping:
            grant[grant_mapping[label]] = value
        elif label.__eq__('Date Awarded'):
            awarddate = value
            action['startTime'] = get_grant_date(value) + 'T00:00:00'
        elif label.__eq__('Term'):
            duration = value
    action['endTime'] = calculate_end_date(awarddate, duration) + 'T00:00:00'
    grant['isAwardedBy'] = awarder
    grant['sameAs'] = url
    action['isFundedBy'] = [grant]

    return json.dumps(action, indent=2)


def calculate_end_date(awarddate, duration):
    match = re.search(month_duration_regex, duration)
    if match.group(1):
        months = int(match.group(1))
    awarddate_split = awarddate.split('/')
    months += int(awarddate_split[0])
    days = int(awarddate_split[1])
    while days > 0:
        try:
            date = datetime.date(int(awarddate_split[2]) + months/12, ((months-1)%12+1), days).isoformat()
            return date
        except ValueError, e:
            if str(e).__eq__('day is out of range for month'):
                days -= 1
            else:
                raise ValueError('Could not calculate end date: ' + str(e))
    return None


def put_agent(agent_uuid, grantee_id, soup, location):
    agent_detailled = {}
    put_uuid('hewlett_grantee_' + grantee_id, agent_uuid)
    agent_detailled['@type'] = 'Organization'
    if hasattr(soup, 'h1'):
        agent_detailled['name'] = [{
            "@language": "en",
            "@value": soup.find('h1').getText()
        }]
    address_tags = soup.findAll('div', { "class" : "aboutgrantee-address" })
    for address_tag in address_tags:
        address_no_span = re.search(address_without_span_regex, (str(address_tag)))
        address_clean = re.sub(address_split_regex, ", ", address_no_span.group(0))
        extract_address(address_clean)
    location['address'] = address
    agent_detailled['location'] = location
    agent_detailled['@id'] = agent_uuid
    agent_detailled['url'] = get_grantee_external_url(soup)
    agents.append(agent_detailled)


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
    count = 1
    output_file.write("[")
    for import_entry in imports:
        output_file.write(import_entry)
        if count < len(imports):
            output_file.write(",\n")
            count += 1
    output_file.write("]")
    output_file.close()


def main():
    global agents
    if len(sys.argv) != 3:
        print 'Usage: python <path>/<to>/import.py <import_url> <path>/<to>/<destination_file.json>'
        # typical usage:
        # python import/hewlett/import.py 'http://www.hewlett.org/grants/?search=oer' 'import/hewlett/search_oer.json'
        # python import/hewlett/import.py 'http://www.hewlett.org/grants/?search=open+educational+resources' 'import/hewlett/search_open_educational_resources.json'
        return
    load_ids_file()
    load_agents_file()
    fill_subdivision_list()
    imports = import_hewlett_data(sys.argv[1])
    print('Hewlett import: found ' + `len(imports)` + ' items.')
    write_into_file(imports, sys.argv[2])
    save_ids_file()
    save_agents_file()


if __name__ == "__main__":
    main()
