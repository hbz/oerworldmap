import BeautifulSoup, urllib2, json, re, os, sys, urlparse, pycountry, datetime, base64, urllib

def read_content_from_url(url):
    try:
        content = urllib2.urlopen(url).read()
    except urllib2.URLError, e:
        if hasattr(e, 'reason'):
            print 'We failed to reach a server.'
            print 'Reason: ', e.reason
        elif hasattr(e, 'code'):
            print 'The server couldn\'t fulfill the request.'
            print 'Error code: ', e.code
        return ""
    return content
