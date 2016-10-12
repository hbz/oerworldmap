import BeautifulSoup, urllib2, json, re, os, sys, uuid, urlparse


grant_mapping = {
    'Amount:': 'frapo:hasMonetaryValue',
    'Date of Award:': 'frapo:hasAwardDate',
    'Term of Grant:': 'schema:duration',
    'Program:': 'frapo:BudgetCategory',
    'Region:': 'frapo:hasLocation',
    'Grant Purpose:': 'frapo:Endeavour',
    'Grant Description:': 'frapo:BudgetInformation',
    'Grantee Website:': 'frapo:hasDomainName'
}


page_regex = re.compile(r"&page=([0-9]+)$")


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
    parsed_url = urlparse(url)
    path = parsed_url[2]



def collect(url):
    result = get_header()
    grant = {
        "@type": "frapo:Grant",
    }
    soup = get_soup_from_page(url)
    if hasattr(soup, 'tbody'):
        for table in soup.findAll('tbody'):
            for row in table.findAll('tr'):
                entry = row.findAll('td')
                field = entry[0].getText()
                if field in grant_mapping:
                    grant[grant_mapping[field]] = entry[1].getText()
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


def import_hewlett_data(url):
    imports = []
    current_number = get_page_number(url)
    while 1:
        links = next_page("http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148", current_number)
        if links.__eq__([]):
            break
        for link in links:
            json = collect(link)
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
        # python import/hewlett/import.py 'http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148' 'import/hewlett/testconsole_01.json'
        return
    imports = import_hewlett_data(sys.argv[1])
    write_into_file(imports, sys.argv[2])

if __name__ == "__main__":
    main()
