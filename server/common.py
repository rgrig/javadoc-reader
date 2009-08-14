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
  last_slot = db.IntegerProperty(required=True)

class WorkingSetPage(webapp.RequestHandler):
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
      # todo: update user ALL (put code in separate method)
      if not pkg: pkg = ''
      qc = UsageCounter.all().filter('url =', url).filter('uid =', uid)
      uc = qc.fetch(1)
      if len(uc) != 1:
        self.response.out.write('unknown uid/url combination')
        self.response.set_status(400, 'unknown uid/url combination')
        return
      qu = Usage.all().filter('uid =',uid).filter('url =',url).filter('slot =',uc[0].last_slot)
      u = qu.fetch(1)
      if len(u) != 1:
        self.response.out.write('this slot should exist')
        self.response.set_status(500, 'this slot should exist')
        return
      u[0].cls = pkg + '/' + cls
      u[0].put()
      uc[0].last_slot = (uc[0].last_slot + 1) % HISTORY_SIZE
      uc[0].put()
      self.response.out.write('touch ' + cls)
    else:
      if uid:
        qu = Usage.all().filter('uid =', uid)
        us = qu.fetch(HISTORY_SIZE)
        cnt = 0
        for u in us:
          if u.cls:
            cnt += 1
        self.response.out.write('%s\n%d\n' % (uid, cnt))
        for u in us:
          if u.cls:
            self.response.out.write(u.cls + '\n')
      else:
        uid = rand()
        for i in range(HISTORY_SIZE):
          us = Usage(url=url,uid=uid,slot=i)
          us.put()
        uc = UsageCounter(url=url,uid=uid,last_slot=0)
        uc.put()
        q = Usage.all().filter('uid =','ALL')
        rs = q.fetch(HISTORY_SIZE)
        if len(rs) != HISTORY_SIZE:
          self.response.out.write('user ALL is not there?\n' + str(rs))
          self.response.set_status(500, 'user ALL is not there?')
          return
        cnt = 0
        for r in rs: 
          if r.cls: 
            cnt = cnt + 1
        self.response.out.write('%s\n%d\n' % (uid, cnt))
        for r in rs: 
          if r.cls:
            self.response.out.write(r.cls + '\n')

application = webapp.WSGIApplication(
                                     [('/workingset', WorkingSetPage)],
                                     debug=True)

def main():
  run_wsgi_app(application)

if __name__ == "__main__":
  main()
 
