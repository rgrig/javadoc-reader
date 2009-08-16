# This is responsible for indexing javadocs.
#
# Parameters:
#   url = the base URL for the javadoc to index
#   force = will not use memcache if this is set
#
# The information is extracted from {url}/allclasses-frame.html
# The index has (exactly) the format:
# <size_of_buffer><NL>
# <buffer>
# <count_of_units><NL>
# <unit_start><SPACE><unit_type><SPACE><unit_parent><NL>
# <suffix_array_of_buffer><NL>
#
# The format is readable but rigid, so that parsing is fast.
# The <buffer> contains the canonical names of all units
# in lexicographic order. Each canonical name looks like
# java.util/Map.Entry and ends with '\n'. For consistency, '/'
# is always present. (The alphabetic order we use is AaBbCc...
# This way it's easy to do case sensitive/insensitive substring
# search.) The class Foo in the default package is "/Foo";
# The package java.util is "java.util/". The client needs not
# split the buffer in pieces while doing the initial parsing
# since the <size_of_buffer> is given.
#
# Where in the buffer a certain unit can be found is given
# by the index <unit_start>. The <unit_type>s are:
#   ANNOTATION
#   CLASS
#   ENUM
#   INTREFACE
#   PACKAGE
# a unit's parent is a 0-based index in the unit list, or '*'
# if there is no parent.
#
# Finally, <suffix_array_of_buffer> is a <NL>-separated list
# of indexes in <buffer>.
#
# NOTE: We rely on the index being sent gzipped, so we don't try
#       eliminate redundancy ourselves to minimize network time.

from HTMLParser import HTMLParser
from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from time import time

class ClassListParser(HTMLParser):
  def __init__(self):
    HTMLParser.__init__(self)
    self.in_a = False
    self.packages = set()
    self.units = []
    self.package = ''
    self.type = ''

  def handle_starttag(self, tag, attrs):
    if tag == 'a':
      for k, v in attrs:
        if k == 'title':
          title = v.split()
          self.package = title[-1]
          self.packages.add(self.package)
          self.type = title[0].upper()
      self.in_a = True 
      
  def handle_data(self, data):
    if self.in_a:
      self.units += [(self.package + '/' + data, self.type)]
      self.in_a = False
    
  def result(self):
    # construct buffer
    start_construct_buffer = time()
    for p in self.packages:
      self.units += [(p + '/', 'PACKAGE')]
    self.units.sort()
    idx = 0
    idx_type = []
    buffer = ''
    for u in self.units:
      idx_type += [(idx, u[1])]
      idx += len(u[0]) + 1
      buffer += u[0] + '\n'
    stop_construct_buffer = time()

    # construct the suffix array
    sa = []

    # format everything
    r = str(len(buffer)) + '\n'
    r += buffer
    r += str(len(idx_type)) + '\n'
    for it in idx_type:
      r += str(it[0]) + ' ' + it[1] + '\n'
    for s in sa:
      r += str(s) + '\n'
    r += 'construct buffer time: ' + str(stop_construct_buffer - start_construct_buffer) + '\n'
    return r

class MainPage(webapp.RequestHandler):
  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'
    url = self.request.get('url') + '/allclasses-frame.html'
    force = self.request.get('force')
    data = memcache.get('url::' + url)
    r = '0\n0\n'
    if data and not force: 
      r = data
    else:
      try:
        fr = urlfetch.fetch(url)
        if fr.status_code == 200:
          p = ClassListParser()
          p.feed(fr.content)
          r = p.result()
          memcache.add('url::' + url, r, 604800) # remember at most one week
      except:
        r = '0\n0\n'
    self.response.out.write(r)

application = webapp.WSGIApplication(
                                     [('/fetch', MainPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
