import BeautifulSoup, urllib2, json

grant_mapping = {
    'Amount:': 'frapo:hasMonetaryValue',
    'Date of Award:': 'frapo:dateAwarded'
}



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
    page = urllib2.urlopen(url).read()
    soup = BeautifulSoup.BeautifulSoup(page)
    soup.prettify()
    for table in soup.findAll('tbody'):
        for row in table.findAll('tr'):
            entry = row.findAll('td')
            field = entry[0].getText()
            if field in grant_mapping:
                grant[grant_mapping[field]] = entry[1].getText()
    result['frapo:awards'] = grant
    print json.dumps(result, indent=2)

if __name__ == "__main__":
    collect("http://www.hewlett.org/grants/16045/monterey-institute-technology-and-education")
