__author__ = 'fo'

from pyld import jsonld
import json, getopt, sys, urllib2, rdflib

def convert(input_path, output_path):
    skos = jsonld.from_rdf(get_skos(input_path))
    context = {
        "@vocab": "http://www.w3.org/2004/02/skos/core#",
        "name": {
            "@id": "http://www.w3.org/2004/02/skos/core#prefLabel",
            "@container": "@set"
        },
        "narrower": {
            "@id": "http://www.w3.org/2004/02/skos/core#narrower",
            "@container": "@set"
        },
        "description": {
            "@id": "http://purl.org/dc/terms/description",
            "@container": "@set"
        },
        "publisher": "http://purl.org/dc/terms/publisher",
        "alternateName": "http://www.w3.org/2004/02/skos/core#altLabel",
        "title": "http://purl.org/dc/terms/title",
        "preferredNamespacePrefix": "http://purl.org/vocab/vann/preferredNamespacePrefix",
        "preferredNamespaceUri": "http://purl.org/vocab/vann/preferredNamespaceUri",
        "source": "http://purl.org/dc/terms/source"
    }
    frame = {
        "@context": context,
        "@type": "ConceptScheme",
        "@explicit": True,
        "hasTopConcept": {
            "@type": "Concept",
            "narrower": {
                "@type": "Concept"
            }
        }
    }
    framed = jsonld.compact(jsonld.frame(skos, frame), context)
    with open(output_path, 'w') as output_file:
        json.dump(framed, output_file, indent=2)
    print "Wrote data for " + input_path + " to " + output_path

def get_skos(input_path):
    with open(input_path, "r") as skos_file:
        return rdflib.Graph().parse(source=skos_file, format='turtle').serialize(format='nt')

if __name__ == "__main__":
    input_path = ''
    output_path = ''
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hi:o:", ["ifile=", "ofile="])
    except getopt.GetoptError:
        print '1.py -i <inputfile> -o <outputfile>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print '1.py -i <inputfile> -o <outputfile>'
            sys.exit()
        elif opt in ("-i", "--ifile"):
            input_path = arg
        elif opt in ("-o", "--ofile"):
            output_path = arg
    convert(input_path, output_path)
