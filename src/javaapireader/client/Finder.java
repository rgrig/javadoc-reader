package javaapireader.client;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;

/** Implements the searching logic. That means it is responsible
    for interpreting the search string. */
public class Finder {
  private List<? extends Unit> hay;
  private String needle;
  private Iterator<? extends Unit> iterator;
  private Unit leftOver;

  public void needle(String needle) {
    this.needle = needle;
    if (hay != null) iterator = hay.iterator();
    leftOver = null;
  }

  public void hay(List<? extends Unit> hay) {
    assert hay != null;
    this.hay = hay;
    iterator = hay.iterator();
    leftOver = null;
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
      if (u.rep().matches(needle)) {
        ++found;
        result.add(u.link());
      }
    }
    leftOver = null;
    while (iterator.hasNext()) {
      Unit u = iterator.next();
      if (u.rep().matches(needle)) {
        leftOver = u;
        return true;
      }
    }
    return false;
  }
}
