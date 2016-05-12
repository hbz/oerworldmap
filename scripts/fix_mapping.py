__author__ = 'fo'

import sys, getopt, json, os, urllib2


def fix(endpoint_url, index_name, output_file):
    mapping = fix_mappings(get_mapping(endpoint_url, index_name))
    with open(output_file, 'w') as fixed_mapping:
        json.dump(mapping[index_name], fixed_mapping, indent=2, separators=(',', ': '))


def fix_mappings(json):
    for index in json:
        json[index] = process_index(json[index])
    return json


def process_index(index):
    for mapping in index:
        index[mapping] = process_mapping(index[mapping])
    return index


def process_mapping(mapping):
    for properties in mapping:
        mapping[properties]['properties'] = process_properties(mapping[properties]['properties'])
    return mapping


def process_properties(properties):
    not_analyzed = ['@id', '@type', '@context', '@language', 'addressCountry', 'email', 'url', 'image',
                    'availableLanguage', 'prefLabel', 'postalCode', 'startDate', 'endDate', 'startTime', 'endTime']
    for property in properties:
        if property in not_analyzed:
            properties[property] = set_not_analyzed(properties[property])
        elif 'properties' in properties[property]:
            # Add a location field to all top-level types, populated by copy_to
            if 'about' == property and not 'location' in properties[property]:
                properties[property]['properties']['location'] = build_location_properties()
            elif 'location' == property:
                properties[property] = copy_to(properties[property], 'about.location')
            properties[property]['properties'] = process_properties(properties[property]['properties'])

    return properties

def copy_to(field, path):
    for property in field['properties']:
        if 'properties' in field['properties'][property]:
            field['properties'][property] = copy_to(field['properties'][property], path + '.' + property)
        else:
            field['properties'][property]['copy_to'] = path + '.' + property
    return field

def build_location_properties():
    return {
        "properties": {
            "geo": {
                "properties": {
                    "lat": {
                        "type": "double"
                    },
                    "lon": {
                        "type": "double"
                    }
                }
            },
            "@type": {
                "index": "not_analyzed",
                "type": "string"
            },
            "address": {
                "properties": {
                    "addressLocality": {
                        "type": "string"
                    },
                    "addressCountry": {
                        "index": "not_analyzed",
                        "type": "string"
                    },
                    "addressRegion": {
                        "type": "string"
                    },
                    "streetAddress": {
                        "type": "string"
                    },
                    "postalCode": {
                        "type": "string"
                    },
                    "@type": {
                        "index": "not_analyzed",
                        "type": "string"
                    }
                }
            }
        }
    }

def set_not_analyzed(field):
    field['type'] = 'string'
    field['index'] = 'not_analyzed'
    return field


def get_mapping(endpoint, index):
    response = urllib2.urlopen(endpoint + '/' + index + '/_mapping')
    return json.loads(response.read())


if __name__ == "__main__":
    endpoint_url = ''
    index_name = ''
    output_file = ''
    try:
        opts, args = getopt.getopt(sys.argv[1:], "he:i:o:", ["endpoint_url=", "output_file="])
    except getopt.GetoptError:
        print '1.py -e <endpoint_url> -i <index_name> -o <output_file>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print '1.py -e <endpoint_url> -i <index_name> -o <output_file>'
            sys.exit()
        elif opt in ("-e", "--endpoint_url"):
            endpoint_url = arg
        elif opt in ("-i", "--index_name"):
            index_name = arg
        elif opt in ("-o", "--output_file"):
            output_file = arg
    fix(endpoint_url, index_name, output_file)
