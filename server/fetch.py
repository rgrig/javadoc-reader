from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.api import urlfetch
from google.appengine.api import memcache
from HTMLParser import HTMLParser

class ClassListParser(HTMLParser):
  def __init__(self):
    HTMLParser.__init__(self)
    self.in_a = False
    self.packages = set()
    self.classes = dict()
    self.current_package = None
    self.is_interface = False

  def handle_starttag(self, tag, attrs):
    if tag == 'a':
      for k, v in attrs:
        if k == 'title':
          title = v.split()
          self.current_package = title[-1]
          self.is_interface = title[0] == "interface"
          self.packages.add(self.current_package)
      self.in_a = True 
      
  def handle_data(self, data):
    if self.in_a:
      cp = (data, self.current_package)
      self.classes[cp] = self.is_interface
      self.in_a = False
    
  def result(self):
    ps = list(self.packages)
    ps.sort()
    pidx = dict()
    i = 0
    for p in ps:
      pidx[p] = i
      i += 1
    cs = list(self.classes)
    cs.sort()
    r = ''
    r += str(len(ps)) + ' ' + str(len(cs)) + '\n'
    for p in ps: r += p + '\n'
    for c, p in cs:
      r += c
      if self.classes[(c, p)]: r += ' 1'
      else: r += ' 0'
      r += ' ' + str(pidx[p]) + '\n'
    return r

class MainPage(webapp.RequestHandler):
  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'
    url = self.request.get('url') + '/allclasses-frame.html'
    data = memcache.get('url::' + url)
    r = '0 0\n'
    if data is not None: r = data
    else:
      fr = urlfetch.fetch(url)
      if fr.status_code == 200:
        p = ClassListParser()
        p.feed(fr.content)
        r = p.result()
        memcache.add('url::' + url, r, 3600)
    self.response.out.write(r)

application = webapp.WSGIApplication(
                                     [('/fetch', MainPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
