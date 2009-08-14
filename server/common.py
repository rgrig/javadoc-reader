# Parameters:
#   url = identifies the javadoc (required)
#   uid = user id (required if cls is given)
#   pkg = the package name
#   cls = the class name (required if pkg is given)
#
# If cls is given then make a note that uid used the class.
# (If pkg is missing, that is the same as the default package "".)
# If cls is not given then
#   If uid is given then return the last classes used by uid.
#   Otherwise, return the last classes used by anyone.
#
# In the second case (cls not given) the reply has the format:
# <uid>
# <class_count>
# <package>/<class>
# ...
# <package>/<class>
#
# Note that the <package> may be missing on some lines (for default).
# The uid is the same as the one given by the user *if* the user gave one.

from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
import random

HISTORY_SIZE = 3 # slots are from 0 to HISTORY_SIZE-1

def rand():
  return hex(random.randint(0,2**64-1))[2:-1]

class Usage(db.Model):
  uid = db.StringProperty(required=True)
  url = db.StringProperty(required=True)
  slot = db.IntegerProperty(required=True)
  cls = db.StringProperty()  # <package>/<class>; slot may be empty

class UsageCounter(db.Model):
  uid = db.StringProperty(required=True)
  url = db.StringProperty(required=True)
  next_slot = db.IntegerProperty(required=True)

class WorkingSetPage(webapp.RequestHandler):
  def getClasses(self, uid, url):
    usages = Usage.all().filter(
        'uid =', uid).filter('url =', url).fetch(HISTORY_SIZE)
    classes = []
    for u in usages:
      if u.cls:
        classes += [u.cls]
    counters = UsageCounter.all().filter(
        'uid =', uid).filter('url =', url).fetch(1) 
    if (len(counters) > 0):
      ns = counters[0].next_slot
      classes = classes[ns:] + classes[:ns]
    return classes

  def touchClass(self, uid, url, cls):
    counters = UsageCounter.all().filter(
        'uid =', uid).filter('url =', url).fetch(1)
    if len(counters) == 0:
      counters.append(UsageCounter(uid=uid, url=url, next_slot=0))
    slot = counters[0].next_slot
    usages = Usage.all().filter(
        'uid =', uid).filter('url =', url).filter('slot =', slot).fetch(1)
    if len(usages) == 0:
      usages.append(Usage(uid=uid, url=url, slot=slot, cls=cls))
    else:
      usages[0].cls = cls
    usages[0].put()
    counters[0].next_slot = (slot + 1) % HISTORY_SIZE
    counters[0].put()
    self.response.out.write('touch uid=%s url=%s cls=%s\n' % (uid, url, cls))

  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'
    url = self.request.get('url')
    uid = self.request.get('uid')
    pkg = self.request.get('pkg')
    cls = self.request.get('cls')
    if not url or (cls and not uid) or (pkg and not cls):
      self.response.set_status(400, 'some required params are missing')
      return
    if cls:
      if not pkg: 
        pkg = ''
      cls = pkg + '/' + cls
      self.touchClass(uid, url, cls)
      self.touchClass('ALL', url, cls)
    else:
      if uid:
        classes = self.getClasses(uid, url)
        if len(classes) < HISTORY_SIZE:
          more_classes = self.getClasses('ALL', url)
          classes = (classes + more_classes)[:HISTORY_SIZE]
        self.response.out.write('%s\n%d\n' % (uid, len(classes)))
        for c in classes:
          self.response.out.write(c + '\n')
      else:
        uid = rand()
        classes = self.getClasses('ALL', url)
        self.response.out.write('%s\n%d\n' % (uid, len(classes)))
        for c in classes: 
          self.response.out.write(c + '\n')

application = webapp.WSGIApplication(
                                     [('/workingset', WorkingSetPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
 
