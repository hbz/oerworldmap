import BeautifulSoup, urllib2, json, re, os, sys


grant_mapping = {
    'Amount:': 'frapo:hasMonetaryValue',
    'Date of Award:': 'frapo:dateAwarded',
    'Term of Grant:': 'frapo:hasTemporalDuration',
    'Program:': 'frapo:BudgetCategory',
    'Region:': 'frapo:hasLocation',
    'Grant Purpose:': 'frapo:Endeavour',
    'Grant Description:': 'frapo:BudgetInformation',
    'Grantee Website:': 'frapo:hasDominName'
}


def get_trailing_page(s):
    m = re.search(r'page[=]\d+$', s)
    return int(m.group()) if m else None


def get_trailing_number(s):
    # from: http://stackoverflow.com/a/7085715/4420271
    m = re.search(r'\d+$', s)
    return int(m.group()) if m else 0


def remove_trailing(astring, trailing):
    # http://stackoverflow.com/a/3663767/4420271 ("jack1")
    regex = r'%s$' % re.escape(trailing)
    return re.sub(regex, '', astring)


def getSoupFromPage(url):
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


def collect(url):
    result = {
        "@id":"urn:uuid:0801e4d4-3c7e-11e5-9f0e-54ee7558c81f",
        "@type":"schema:Organization",
        "schema:name": [
            {
                "@language":"en",
                "@value":"William and Flora Hewlett Foundation"
            }
        ]
    }
    grant = {
        "@type": "frapo:Grant",
    }
    soup = getSoupFromPage(url)
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
    soup = getSoupFromPage(url)
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


def import_hewlett_data(url):
    imports = []
    current_number = 0
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
