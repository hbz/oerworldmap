import BeautifulSoup, urllib2, json, re, os

grant_mapping = {
    'Amount:': 'frapo:hasMonetaryValue',
    'Date of Award:': 'frapo:dateAwarded'
}

def getSoupFromPage(url):
    page = urllib2.urlopen(url).read()
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
    for table in soup.findAll('tbody'):
        for row in table.findAll('tr'):
            entry = row.findAll('td')
            field = entry[0].getText()
            if field in grant_mapping:
                grant[grant_mapping[field]] = entry[1].getText()
    result['frapo:awards'] = grant
    print json.dumps(result, indent=2)

def crawlPage(url):
    links = []
    soup = getSoupFromPage(url)
    for table in soup.findAll('tbody'):
        for row in table.findAll('tr'):
            cols = row.findAll('td')
            link = cols[0].find('a').get('href')
            links.append('http://www.hewlett.org' + link)
    return links

def nextPage(currentPage):
    m = re.search(r'page=\d+$', currentPage)
    if m is None:
        return currentPage + '&page=1'
    else:
        next = "http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148&page=%d"
        i = 2
        if os.path.exists(next % i):
            return next % i

if __name__ == "__main__":
    print nextPage("http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148&page=1")
    links = crawlPage("http://www.hewlett.org/grants/search?order=field_date_of_award&sort=desc&keywords=OER&year=&term_node_tid_depth_1=All&program_id=148")
    for link in links:
        collect(link)
