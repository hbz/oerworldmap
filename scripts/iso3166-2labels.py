import requests, sys, json

query = '''
PREFIX wdt: <http://www.wikidata.org/prop/direct/>
PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>

SELECT ?code ?label WHERE {
  ?item wdt:P300 ?code .
  ?item rdfs:label ?label .
  FILTER (LANG(?label) = "%s")
}
''' % sys.argv[1]

url = 'https://query.wikidata.org/bigdata/namespace/wdq/sparql'
data = requests.get(url, params={'query': query, 'format': 'json'}).json()

for binding in data["results"]["bindings"]:
    print (u".".join(binding["code"]["value"].split("-")) + "=" + binding["label"]["value"]).encode('utf-8').strip()
