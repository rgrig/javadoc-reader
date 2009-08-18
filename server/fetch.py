# This is responsible for indexing javadocs.
#
# Parameters:
#   url1, url2, ..., urlN = the javadocs to index
#   force = will not use memcache if this is set (to anything)
#
# The information is extracted from {url}/allclasses-frame.html
# The index has (exactly) the format:
# <size_of_buffer><NL>
# <buffer>
# <count_of_javadocs>
# <javadoc_url><NL> <*>
# <count_of_units><NL>
# <unit_start><SPACE><unit_type><SPACE><unit_parent>
#     <SPACE><unit_javadoc><NL> <*>
# <suffix_array_of_buffer><NL>
# <debugging_info>
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
# Where in the buffer a certain unit can be found is given by the
# index <unit_start>. A unit's parent is a 0-based index in the
# unit list, or '*' if there is no parent.
#
# Finally, <suffix_array_of_buffer> is a <NL>-separated list
# of indexes in <buffer>.
#
# NOTE: We rely on the index being sent gzipped, so we don't try
#       eliminate redundancy ourselves to minimize network time.

from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from HTMLParser import HTMLParser
from operator import indexOf
from time import time

# Does basic parsing of 'allclasses-frame.html'
class ClassListParser(HTMLParser):
  def process(self, html):
    self.units = dict()
    self.package = ''
    self.type = ''
    self.in_a = False
    self.feed(html)
    return self.units

  def handle_starttag(self, tag, attrs):
    if tag == 'a':
      for k, v in attrs:
        if k == 'title':
          title = v.split()
          self.package = title[-1]
          if self.package not in self.units:
            self.units[self.package] = []
          self.type = title[0]
      self.in_a = True 
      
  def handle_data(self, data):
    if self.in_a:
      if self.package:
        self.units[self.package] += [(data, self.type)]
      self.in_a = False

class MainPage(webapp.RequestHandler):
  def index(self):
    self.time['index'] = time()
    self.units = []
    self.javadocs = list(self.raw.keys())
    self.javadocs.sort()
    for jd, x in self.raw.items():
      jdi = indexOf(self.javadocs, jd)
      for p, l in x.items():
        self.units += [(p + '/', 'package', jdi)]
        for c, t in l:
          self.units += [(p + '/' + c, t, jdi)]
    self.units.sort()

    self.time['index'] = time() - self.time['index']

  def send_answer(self):
    self.time['send_answer'] = time()
    sz = 0
    for un, _, _ in self.units:
      sz += len(un) + 1
    self.response.out.write(str(sz) + '\n')
    for un, _, _ in self.units:
      self.response.out.write(un + '\n')
    self.response.out.write(str(len(self.javadocs)) + '\n')
    for jd in self.javadocs:
      self.response.out.write(jd + '\n')
    self.response.out.write(str(len(self.units)) + '\n')
    idx = 0
    for un, ut, uj in self.units:
      self.response.out.write(str(idx) + ' ' + ut + ' ' + str(uj) + '\n')
      idx += len(un) + 1
    # TODO(radugrigore): send suffix array
    self.time['send_answer'] = time() - self.time['send_answer']

  def send_debug(self):
    self.response.out.write('\n')
    for category, t in self.time.items():
      self.response.out.write(category + ' ' + str(t) + '\n')

  def get(self):
    self.time = dict()
    self.time['collect'] = time()
    self.response.headers['Content-Type'] = 'text/plain'
    force = self.request.get('force')
    i = 0
    parser = ClassListParser()
    self.raw = dict()
    while True:
      javadoc = self.request.get('url' + str(i))
      if not javadoc: 
        break
      url = javadoc + '/allclasses-frame.html'
      data = None
      if not force: 
        data = memcache.get('url::' + javadoc)
        self.response.out.write(javadoc + ' cached\n')
      if data is None:
        try:
          fetch_result = urlfetch.fetch(url)
          if fetch_result.status_code == 200:
            data = parser.process(fetch_result.content)
            self.response.out.write(javadoc + ' computed\n')
          memcache.add('url::' + javadoc, data, 604800) # remember <= 1 week
        except:
          pass
      if data:
        self.raw[javadoc] = data
      i += 1
    self.time['collect'] = time() - self.time['collect']
    self.index()
    self.send_answer()
    self.send_debug()

application = webapp.WSGIApplication(
                                     [('/fetch', MainPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
