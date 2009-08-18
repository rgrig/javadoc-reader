# This is responsible for indexing javadocs.
#
# Parameters:
#   url0 url1, ... = the javadocs to index
#   force = will not use memcache if this is set (to anything)
#
# The information is extracted from {url}/allclasses-frame.html
# The index has (exactly) the format:
# <size_of_buffer><NL>
# <buffer>
# <count_of_javadocs>
# <javadoc_url><NL> <*>
# <count_of_units><NL>
# <unit_start><SPACE><unit_type><SPACE><SPACE><unit_javadoc><NL> <*>
# <suffix_array_of_buffer><NL>
# <debugging_info>
#
# The format is readable (for debugging) but rigid (for parsing
# speed). To help parsing each sequence is preceded by its
# length. (This means that no 'growing' is needed on the client
# side and no 'end-of-sequence-marker' check is necessary.)
#
# There are four sections:
#  (1) buffer
#  (2) javadocs
#  (3) units
#  (4) suffix array
# Everything that follows is ignored and can be used for
# debugging.
#
# The buffer contains all the canonical names, sorted
# lexicographically. Its size is given in characters (bytes). A
# few conventions about the alphabetic order are important for
# the client. Uppercase and lowercase versions of the same letter
# must have adjacent ranks (to facilitate case sensitive, versus
# case insensitive search); the characters '.' and '/' must have
# adjacent ranks (because most of the time the client will *not*
# want to distinguish them). Each canonical name (like
# java.util/Map.Entry) is followed by '\n'.
#
# Javadocs are listed in some arbitrary order one per line.
# The purpose is to be able to refer to them by index later.
#
# Each unit has a type (package, interface, etc.), an index to
# its representation in the buffer, and the index of its javadoc.
# Units are guaranteed to appear in increasing order of their
# buffer index (aka, lexicographic order).
#
# The suffix array is a list of indexes in the buffer, for now.
# Its length is the same as that of the buffer, so it is not
# given.
#
# NOTE: We rely on the index being sent gzipped, so we don't try
#       eliminate redundancy ourselves to minimize network time.

from HTMLParser import HTMLParser
from google.appengine.api import memcache
from google.appengine.api import urlfetch
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from operator import indexOf
from string import ascii_lowercase
from time import time

### suffix array computation
idx_dict = dict()
for i in range(len(ascii_lowercase)):
  idx_dict[ascii_lowercase[i]] = i + 3
idx_dict['.'] = idx_dict['/'] = 2
idx_dict['\n'] = 1
def idx(c):
  c = c.lower()
  if c not in idx_dict:
    return len(ascii_lowercase) + 3
  return idx_dict[c]

def cmp3(a, i, j):
  if a[i] != a[j]: return a[i] - a[j]
  if a[i+1] != a[j+1]: return a[i+1] - a[j+1]
  return a[i+2] - a[j+2]

def cmp2(a, r, i, j):
  if i % 3 != 0 and j % 3 != 0: return r[i] - r[j]
  if a[i] != a[j]: return a[i] - a[j]
  return cmp2(a, r, i + 1, j + 1)

def sample(l, m):
  return [x for x in range(l + 1) if x % 3 == m]

# assumes that a contains only positive integers
def sa(a):
  l = len(a)
  a += [0, 0, 0, -1]
  s23 = sample(l, 1) + sample(l, 2)
  s23.sort(lambda i, j: cmp3(a, i, j))
  r, previous, distinct = 0, l + 1, True
  rank = dict()
  for s in s23:
    if cmp3(a, previous, s) != 0:
      r += 1
    else:
      distinct = False
    rank[s] = r
    previous = s
  if not distinct:
    s23 = sample(l, 1) + sample(l, 2)
    sa23 = sa([rank[x] for x in s23])[1:]
    r = 1
    for i in range(len(sa23)):
      rank[s23[sa23[i]]] = r
      r += 1
    s23.sort(lambda i, j: rank[i] - rank[j])
  rank[l+1] = 0
  s1 = sample(l, 0)
  s1.sort(lambda i, j: cmp2(a, rank, i, j))
  result, i, j, k = [0] * (l + 1), 0, 0, 0
  while i < len(s1) and j < len(s23):
    if cmp2(a, rank, s1[i], s23[j]) < 0:
      result[k] = s1[i]
      i += 1
    else:
      result[k] = s23[j]
      j += 1
    k += 1
  result[k:] = s1[i:] + s23[j:]
  return result

### Does basic parsing of 'allclasses-frame.html'
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

### Main loop
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
    self.suffix_array = []
    for un, _, _ in self.units:
      self.suffix_array += [idx(c) for c in un + '\n']
    self.suffix_array = sa(self.suffix_array)
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
    for si in self.suffix_array[1:]:
      self.response.out.write(str(si) + '\n')
    self.time['send_answer'] = time() - self.time['send_answer']

  def send_debug(self):
    self.response.out.write('\n')
    for category, t in self.time.items():
      self.response.out.write(category + ' ' + str(t) + '\n')
    for jd in self.jds:
      self.response.out.write(jd + '\n')
  
  def get(self):
    self.reply()

  def post(self):
    self.reply()

  def reply(self):
    self.time = dict()
    self.time['collect'] = time()
    self.response.headers['Content-Type'] = 'text/plain'
    force = self.request.get('force')
    i = 0
    parser = ClassListParser()
    self.raw = dict()
    self.jds = []
    while True:
      # TODO(radugrigore): use get_all instead of this hack (in javadoc.py)
      javadoc = self.request.get('url' + str(i))
      self.jds += [javadoc]
      if not javadoc: 
        break
      url = javadoc + '/allclasses-frame.html'
      data = None
      if not force: 
        data = memcache.get('url::' + javadoc)
      if data is None:
        try:
          fetch_result = urlfetch.fetch(url)
          if fetch_result.status_code == 200:
            data = parser.process(fetch_result.content)
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
