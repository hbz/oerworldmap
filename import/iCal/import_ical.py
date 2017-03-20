import BeautifulSoup, urllib2, json, re, os, sys, uuid, urlparse, pycountry, datetime, base64, urllib, StringIO
from ..common.OerWmFiles import *
from ..common.OerWmUrls import *


path = os.path.dirname(os.path.realpath(__file__)) + os.path.sep
uuid_file = path + "id_map.json"
uuids = {}
import_list = []


def read_header(buffer):
    header = []
    line = buffer.readline()
    while line:
        if line.__eq__("BEGIN:VEVENT\n"):
            break
        else:
            header.append(line)
            line = buffer.readline()
    return header


def read_next_event(buffer):
    event = []
    line = buffer.readline()
    while line:
        if line.__eq__("BEGIN:END\n"):
            break
        else:
            event.append(line)
            line = buffer.readline()
    return event


def lines_to_resource(header, event):
    resource = {}
    for line in event:
        if line.startswith("SUMMARY:"):
            resource['@value'] = line
    return resource


def import_ical_from_string(page_content):
    imports = []
    buffer = StringIO.StringIO(page_content)
    header = read_header(buffer)
    event = read_next_event(buffer)
    while event:
        resource = lines_to_resource(header, event)
        imports.append(resource)
        line = buffer.readline()
        if line.__eq__("END:VCALENDAR"):
            break
        else:
            event = read_next_event(buffer)
    return imports


def import_ical_from_url(url):
    # this function expects a url purely providing a or several iCal event(s)
    page_content = read_content_from_url(url)
    imports = import_ical_from_string(page_content)
    return imports


def main():
    global path, uuids, import_list
    if len(sys.argv) != 4:
        print 'Usage: python -m import.iCal.import_ical <path>/<to>/import_ical.py <import_url> <path>/<to>/<destination_file.json>'
        return
    load_ids_from_file(path + "id_map.json", uuids)
    import_list = import_ical_from_url(sys.argv[2])
    save_ids_to_file(uuids, path + "id_map.json")
    return import_list


if __name__ == "__main__":
    import_list = main()
    # write_list_into_file(import_list, sys.argv[3])
    print "Events: " + `import_list`
