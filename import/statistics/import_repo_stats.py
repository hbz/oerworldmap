import urllib2, json, re, os, sys, codecs, requests
from elasticsearch import Elasticsearch

data_sources = []
data = []


def get_forbidden_indices():
    forbidden_indices = []
    forbidden_indices.append(get_field_from_conf('es.index.name', get_script_path() + '/../../conf/application.conf'))
    forbidden_indices.append(get_field_from_conf('es.index.name', get_script_path() + '/../../conf/test.conf'))
    forbidden_indices.append(get_field_from_conf('es.index.name', get_script_path() + '/../../conf/travis.conf'))
    return forbidden_indices


def get_field_from_conf(field, file):
    with open(file) as f:
        lines = f.readlines()
        lines = [x.strip() for x in lines]
        for line in lines:
            regex = re.escape(field) + '[\W]*([\w]*)[\W]*'
            m = re.search(regex, line)
            if m:
                return m.group(1)


def get_script_path():
    return os.path.dirname(os.path.realpath(sys.argv[0]))


def store_in_index(date, id, node_url, index):
    # TODO: index handling: index exists, create index etc.
    es = Elasticsearch(node_url)
    es.index(index=index, doc_type="stats", id=id, body=date)


def import_json_content_from_url(url):
    if url.startswith('file://'):
        page_content = read_content_from_file(url, 'utf-8')
    else:
        page_content = read_content_from_url(url)
    return json.loads(page_content)


def read_content_from_url(url):
    try:
        site = urllib2.urlopen(url)
        encoding=site.headers['content-type'].split('charset=')[-1]
        content = site.read()
    except urllib2.URLError, e:
        if hasattr(e, 'reason'):
            print 'We failed to reach a server.'
            print 'Reason: ', e.reason
        elif hasattr(e, 'code'):
            print 'The server couldn\'t fulfill the request.'
            print 'Error code: ', e.code
        return ""
    if encoding.__eq__('text/plain'):
        return content
    return unicode(content, encoding)


def read_content_from_file(file, encoding):
    if file.startswith('file://'):
        file = file[7:]
    f = codecs.open(file, 'r', encoding)
    return f.read()


def collect_data_sources(sources, url):
    sources.append(url)
    # Currently just imports one (dummy) file
    # TODO: automatically generate / fill list


def checkArguments():
    if len(sys.argv) != 5:
        print ('Script call misconfigured.\nUsage: python -m <path>/<to>/import_repo_stats <path>/<to>/import_repo_stats.py <protocoll>://<url>/<to>/<repostats> <protocoll>://<url>/<to>/<elasticsearch_node> <indexname>')
        return False
    if get_forbidden_indices().__contains__(sys.argv[4]):
        print ('Index name ' + `sys.argv[4]` + ' is already in use. Choose another index name.')
        return False
    return True


def main():
    if not checkArguments():
        return
    global data_sources, data
    collect_data_sources(data_sources, sys.argv[2])
    for source in data_sources:
        data.append(import_json_content_from_url(source))
    for date in data:
        id = date['repository']['domain']
        store_in_index(date, id, sys.argv[3], sys.argv[4])


if __name__ == "__main__":
    main()
