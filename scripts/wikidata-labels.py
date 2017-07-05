import requests, sys, json

query = '''
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
PREFIX p: <http://www.wikidata.org/prop/>
PREFIX pq: <http://www.wikidata.org/prop/qualifier/>

SELECT DISTINCT ?code ?label WHERE {
  ?item wdt:%s ?code .
  FILTER NOT EXISTS { ?item p:%s [ pq:P582 ?end ] . }
  ?item rdfs:label ?label .
  FILTER (LANG(?label) = "%s")
} ORDER BY ASC(?code)
''' % (sys.argv[1], sys.argv[1], sys.argv[2])

url = 'https://query.wikidata.org/bigdata/namespace/wdq/sparql'
data = requests.get(url, params={'query': query, 'format': 'json'}).json()

for binding in data["results"]["bindings"]:
    print (u".".join(binding["code"]["value"].split("-")) + "=" + binding["label"]["value"]).encode('utf-8').strip()
