import BeautifulSoup, urllib2, json, re, os, sys, urlparse, pycountry, datetime, base64, urllib, codecs


def load_ids_from_file(id_file, ids):
    if not os.path.isfile(id_file):
        open(id_file, 'a').close()
    try:
        with open(id_file, 'r') as file:
            ids = json.loads(file.read())
    except ValueError, e:
        if not str(e).__eq__('No JSON object could be decoded'):
            raise ValueError('Unexpected error while loading ids file: ' + str(e))
    return ids


def save_ids_to_file(ids, id_file):
    with open(id_file, 'w') as file:
        file.write(json.dumps(ids))


def write_list_into_file(import_list, filename):
    output_file = open(filename, "w")
    count = 1
    output_file.write("[")
    for import_entry in import_list:
        output_file.write(import_entry)
        if count < len(import_entry):
            output_file.write(",\n")
            count += 1
    output_file.write("]")
    output_file.close()


def read_content_from_file(file, encoding):
    if file.startswith('file://'):
        file = file[7:]
    f = codecs.open(file, 'r', encoding)
    return f.read()
