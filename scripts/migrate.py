__author__ = 'fo'

import base64
import getopt
import getpass
import imp
import json
import sys
import urllib2
import urlparse


def migrate(source, target, scripts):
    source_user = raw_input("Source user:")
    source_pass = getpass.getpass('Source password:')
    target_user = raw_input("Target user:")
    target_pass = getpass.getpass('Target password:')
    resources = get_resources(source, source_user, source_pass)
    for resource in resources:
        resource = migrate_resource(resource, scripts)
        post_resource(target, target_pass, target_user, resource)

def get_resources(source, source_user, source_pass):
    print 'GET ' + source
    request = urllib2.Request(source, headers={"Accept" : "application/json"})
    if source_user and source_pass:
        base64string = base64.encodestring('%s:%s' % (source_user, source_pass)).replace('\n', '')
        request.add_header("Authorization", "Basic %s" % base64string)
    response = json.loads(urllib2.urlopen(request).read())
    resources = response['member']
    if response['nextPage']:
        url = urlparse.urlparse(source)
        resources += get_resources(url.scheme + '://' +  url.netloc + response['nextPage'], source_user, source_pass)
    return resources

def migrate_resource(resource, scripts):
    for script in scripts:
        resource = script.migrate(resource)
    return resource

def post_resource(target, target_pass, target_user, resource):
    print 'POST ' + resource['@id'] + ' to ' + target
    request = urllib2.Request(target, json.dumps(resource))
    request.add_header('Content-Type', 'application/json')

    if target_user and target_pass:
        base64string = base64.encodestring('%s:%s' % (target_user, target_pass)).replace('\n', '')
        request.add_header("Authorization", "Basic %s" % base64string)

    try:
        response = urllib2.urlopen(request)
        print response.getcode()
    except urllib2.HTTPError as e:
        print e.getcode()
        print e.read()

if __name__ == "__main__":
    source = ''
    target = ''
    migrations = []
    try:
        opts, args = getopt.getopt(sys.argv[1:], "hs:t:m:", ["source=", "target=", "migrate="])
    except getopt.GetoptError:
        print '1.py -s <source> -t <target> -m <migrate>'
        sys.exit(2)
    for opt, arg in opts:
        if opt == '-h':
            print '1.py -s <source> -t <target> -m <migrate>'
            sys.exit()
        elif opt in ("-s", "--source"):
            source = arg
        elif opt in ("-t", "--target"):
            target = arg
        elif opt in ("-m", "--migrate"):
            migrations.append(arg)
    scripts = []
    for migration in migrations:
        scripts.append(imp.load_source("migrations." + migration, "./migrations/" + migration + ".py"))
    migrate(source, target, scripts)
