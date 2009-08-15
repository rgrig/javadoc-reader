package javaapireader.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;

/** Implements the searching logic. That means it is responsible
    for interpreting the search string. */
public class Finder<T extends Unit> {
  // This seems *much* faster than using anything in the GWT JRE emulation.
  private static class Matcher {
    private Object regex;
    public native void regex(String r) /*-{
      regex = new RegExp(r);
    }-*/;

    public native boolean test(String h) /*-{
      return regex.test(h);
    }-*/;
  }

  private List<T> hay;
  private Iterator<T> iterator;
  private Unit leftOver;
  private Matcher m = new Matcher();

  public void needle(String needle) {
    if (hay != null) iterator = hay.iterator();
    leftOver = null;
    m.regex(needle);
  }

  public void hay(List<T> hay) {
    assert hay != null;
    this.hay = hay;
    iterator = hay.iterator();
    leftOver = null;
  }

  public List<T> hay() { 
    return new ArrayList<T>(hay); 
  }

  /** 
    Puts at most {@code max} elements in {@code result}. Returns
    whether there are more results or not. It continues from
    where the last {@code find()} ended, unless {@code needle()}
    or {@code hay()} was set in the meantime.
   */
  public boolean find(int max, List<HTML> result) {
//Window.alert("DBG: find with needle="+needle);
    assert result != null;
    int found = 0;
    if (leftOver != null) {
      ++found;
      result.add(leftOver.link());
    }
    while (found < max && iterator.hasNext()) {
      Unit u = iterator.next();
      if (m.test(u.rep())) {
        ++found;
        result.add(u.link());
      }
    }
    leftOver = null;
    while (iterator.hasNext()) {
      Unit u = iterator.next();
      if (m.test(u.rep())) {
        leftOver = u;
        return true;
      }
    }
    return false;
  }
}
