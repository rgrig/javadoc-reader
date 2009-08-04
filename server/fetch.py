from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
from google.appengine.api import urlfetch
from HTMLParser import HTMLParser

class ClassListParser(HTMLParser):
  def __init__(self):
    HTMLParser.__init__(self)
    self.ina = False
    self.out = ''

  def handle_starttag(self, tag, attrs):
    if tag == 'a':
      for k, v in attrs:
        if k == 'title':
          self.out += v.split()[-1] + ' '
      self.ina = True 
      
  def handle_data(self, data):
    if self.ina:
      self.out += data + '\n'
    self.ina = False
    
  def result(self):
    return self.out

class MainPage(webapp.RequestHandler):
  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'
    url = self.request.get('url') + '/allclasses-frame.html'
    result = urlfetch.fetch(url)
    if result.status_code == 200:
      p = ClassListParser()
      p.feed(result.content)
      self.response.out.write(p.result())

application = webapp.WSGIApplication(
                                     [('/fetch', MainPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
