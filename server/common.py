# Parameters:
#   url = identifies the javadoc (required)
#   uid = user id (required if unit is given)
#   unit = the package/class
#
# [touch] If unit is given then make a note that uid used it.
# [query] Otherwise
#   If uid is given then return the last units used by uid.
#   Otherwise, return the last units used by ALL.
#
# In the query case (unit not given) the reply has the format:
# <uid>
# <unit_count>
# <unit> ... </unit>
#
# The uid is the same as the one given by the user *if* the user gave one.
#
# Examples of unit encodings (up to the client!):
#   0java.util/ArrayList   (class in a package)
#   0/Foo                  (class in default package)
#   1java.util/List        (interface in a package)
#   1java.util/Map.Entry   (nested interface)
#   2java.util             (package)

from google.appengine.ext import db
from google.appengine.ext import webapp
from google.appengine.ext.webapp.util import run_wsgi_app
import random

HISTORY_SIZE = 400 # slots are from 0 to HISTORY_SIZE-1

def rand():
  return hex(random.randint(0,2**64-1))[2:-1]

class Usage(db.Model):
  uid = db.StringProperty(required=True)
  url = db.StringProperty(required=True)
  slot = db.IntegerProperty(required=True)
  unit = db.StringProperty()  # slot may be empty

class UsageCounter(db.Model):
  uid = db.StringProperty(required=True)
  url = db.StringProperty(required=True)
  next_slot = db.IntegerProperty(required=True)

class WorkingSetPage(webapp.RequestHandler):
  def getUnits(self, uid, url):
    usages = Usage.all().filter(
        'uid =', uid).filter('url =', url).fetch(HISTORY_SIZE)
    units = []
    for u in usages:
      if u.unit:
        units += [u.unit]
    counters = UsageCounter.all().filter(
        'uid =', uid).filter('url =', url).fetch(1)
    if (len(counters) > 0):
      ns = counters[0].next_slot
      units = units[ns:] + units[:ns]
    return units

  def touchUnit(self, uid, url, unit):
    counters = UsageCounter.all().filter(
        'uid =', uid).filter('url =', url).fetch(1)
    if len(counters) == 0:
      counters.append(UsageCounter(uid=uid, url=url, next_slot=0))
    slot = counters[0].next_slot
    usages = Usage.all().filter(
        'uid =', uid).filter('url =', url).filter('slot =', slot).fetch(1)
    if len(usages) == 0:
      usages.append(Usage(uid=uid, url=url, slot=slot, unit=unit))
    else:
      usages[0].unit = unit
    usages[0].put()
    counters[0].next_slot = (slot + 1) % HISTORY_SIZE
    counters[0].put()
    self.response.out.write('touch uid=%s url=%s unit=%s\n' % (uid, url, unit))

  def get(self):
    self.response.headers['Content-Type'] = 'text/plain'
    url = self.request.get('url')
    uid = self.request.get('uid')
    unit = self.request.get('unit')
    if not url or (unit and not uid):
      self.response.set_status(400, 'some required params are missing')
      return
    if unit:
      self.touchUnit(uid, url, unit)
      self.touchUnit('ALL', url, unit)
    else:
      if uid:
        units = self.getUnits(uid, url)
        if len(units) < HISTORY_SIZE:
          more_classes = self.getUnits('ALL', url)
          units = (units + more_classes)[:HISTORY_SIZE]
        self.response.out.write('%s\n%d\n' % (uid, len(units)))
        for c in units:
          self.response.out.write(c + '\n')
      else:
        uid = rand()
        units = self.getUnits('ALL', url)
        self.response.out.write('%s\n%d\n' % (uid, len(units)))
        for c in units:
          self.response.out.write(c + '\n')

application = webapp.WSGIApplication(
                                     [('/workingset', WorkingSetPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()

