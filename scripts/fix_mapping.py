# python scripts/fix_mapping.py -e http://localhost:9200 -i oerworldmap -o conf/index-config.json

__author__ = 'fo'

import sys, getopt, json, os, urllib2


def fix(endpoint_url, index_name, output_file):
    mapping = fix_mappings(get_mapping(endpoint_url, index_name))
    with open(output_file, 'w') as fixed_mapping:
        json.dump(mapping[index_name], fixed_mapping, indent=2, separators=(',', ': '))


def fix_mappings(json):
    for index in json:
        json[index] = process_index(json[index])
        json[index]["settings"] = settings()
    return json


def process_index(index):
    for mapping in index:
        index[mapping] = process_mapping(index[mapping])
    return index


def process_mapping(mapping):
    for properties in mapping:
        mapping[properties]['properties'] = process_properties(mapping[properties]['properties'], False)
        mapping[properties]['transform'] = transform()
    return mapping


def process_properties(properties, is_name_branch):
    not_analyzed = ['@id', '@type', '@context', '@language', 'email', 'url', 'image',
                    'availableLanguage', 'prefLabel', 'postalCode', 'hashtag', 'addressRegion']
    country_name = ['addressCountry']
    ngrams = ['@value']
    name = ['name']
    keywords = ['keywords']
    date_time = ['startDate', 'endDate', 'startTime', 'endTime', 'dateCreated', 'hasAwardDate']
    geo = ['geo']
    for property in properties:
        if property in not_analyzed:
            properties[property] = set_not_analyzed()
        elif property in date_time:
            properties[property] = set_date_time()
        elif property in geo:
            properties[property] = set_geo_point()
        elif property in ngrams:
            if is_name_branch:
                properties[property] = set_ngram("title_analyzer")
            else:
                properties[property] = set_ngram("standard")
        elif property in keywords:
            properties[property] = set_keywords_analyzer()
        elif property in country_name:
            properties[property] = set_country_name()
        elif 'properties' in properties[property]:
            if property in name:
                is_name_branch = True
            properties[property]['properties'] = process_properties(properties[property]['properties'], is_name_branch)

    return properties

def set_not_analyzed():
    return {
        'type': 'string',
        'index': 'not_analyzed'
    }

def set_keywords_analyzer():
    return {
        'type': 'string',
        'analyzer': 'keywords_analyzer'
    }

def set_date_time():
    return {
        'type': 'date',
        'format': 'dateOptionalTime'
    }

def set_geo_point():
    return {
        "type": "geo_point"
    }

def set_ngram(variations_search_analyzer):
    return {
        "type": "multi_field",
        "fields": {
            "@value": {
                "type": "string"
            },
            "variations": {
                "type": "string",
                "analyzer": "title_analyzer",
                "search_analyzer": variations_search_analyzer
            },
            "simple_tokenized": {
                "type": "string",
                "analyzer": "simple",
                "search_analyzer": "standard"
            }
        }
    }

def set_country_name():
    return {
        "type": "multi_field",
        "fields": {
            "addressCountry": {
                "type": "string",
                'index': 'not_analyzed'
            },
            "name": {
                "type": "string",
                "analyzer": "country_synonyms_analyzer",
                "search_analyzer": "country_synonyms_analyzer"
            }
        }
    }

def transform():
    return {
        "script": """
            if (!ctx._source['about']['location']) {

                ctx._source['about']['location'] = [];

                if (ctx._source['about']['provider'] && ctx._source['about']['provider']['location'])
                    ctx._source['about']['location'] << ctx._source['about']['provider']['location'];

                if (ctx._source['about']['agent'] && ctx._source['about']['agent']['location'])
                    ctx._source['about']['location'] << ctx._source['about']['agent']['location'];

                if (ctx._source['about']['participant'] && ctx._source['about']['participant']['location'])
                    ctx._source['about']['location'] << ctx._source['about']['participant']['location'];

                if (ctx._source['about']['member'] && ctx._source['about']['member']['location'])
                    ctx._source['about']['location'] << ctx._source['about']['member']['location'];

                if (ctx._source['about']['mentions'] && ctx._source['about']['mentions']['location'])
                    ctx._source['about']['location'] << ctx._source['about']['mentions']['location'];
            };
        """
    }

def settings():
    with open(sys.path[0] + '/country_synonyms.txt', 'r') as f:
        country_list = f.read().splitlines()
    return {
        "analysis": {
            "filter": {
                "title_filter": {
                    "type": "word_delimiter",
                    "preserve_original": True,
                    "split_on_numerics": False,
                    "split_on_case_change": True,
                    "generate_word_parts": True,
                    "generate_number_parts": False,
                    "catenate_words": True,
                    "catenate_numbers": False,
                    "catenate_all": False
                },
                "asciifolding_preserve_original": {
                    "type": "asciifolding",
                    "preserve_original": True
                },
                "country_synonyms_filter": {
                    "type": "synonym",
                    "synonyms": country_list
                }
            },
            "analyzer": {
                "title_analyzer": {
                    "filter": [
                        "title_filter",
                        "asciifolding_preserve_original",
                        "lowercase"
                    ],
                    "type": "custom",
                    "tokenizer": "hyphen"
                },
                "keywords_analyzer": {
                    "filter": "lowercase",
                    "tokenizer": "keyword"
                },
                "country_synonyms_analyzer": {
                    "tokenizer": "icu_tokenizer",
                    "filter": [
                        "lowercase",
                        "country_synonyms_filter"
                    ]
                }
            }
        }
    }

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
