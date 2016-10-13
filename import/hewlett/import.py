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
        "@id":"urn:uuid:0801e4d4-3c7e-11e5-9f0e-54ee7558c81f",
        "@type":"schema:Organization",
        "schema:name": [
            {
                "@language":"en",
                "@value":"William and Flora Hewlett Foundation"
            }
        ]
    }


def get_uuid(key):
    a_uuid = get_uuid_from_file(key)
    if a_uuid is None:
        a_uuid = uuid.uuid4()
    return a_uuid


def get_uuid_from_file(key):
    # TODO
    # interim function:
    return uuid.uuid4()


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
    grant = {
        "@type": "frapo:Grant",
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
    grant['frapo:hasGrantNumber'] = get_grant_number(url)
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
                    grant[grant_mapping[field]] = entry[1].getText()
                else:
                    if field.__eq__('Grant Description:'):
                        grant['schema:description'] = {
                            "@language":"en",
                            "@value":entry[1].getText()
                        }
                    else:
                        if field.__eq__('Term of Grant:'):
                            duration = get_grant_duration(entry[1].getText())
                            grant['schema:duration'] = duration
                            action['schema:duration'] = duration
                        else:
                            if field.__eq__('Grant Purpose:'):
                                action['schema:name'] = {
                                    "@language": "en",
                                    "@value": entry[1].getText()
                                }
    location['schema:address'] = address
    agent['schema:location'] = location
    action['schema:agent'] = agent
    grant['frapo:funds'] = action
    result['frapo:awards'] = grant
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
    imports = import_hewlett_data(sys.argv[1])
    write_into_file(imports, sys.argv[2])

if __name__ == "__main__":
    main()
