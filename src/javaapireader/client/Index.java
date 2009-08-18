package javaapireader.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;

/** A structure holding the index of a javadoc. */
public final class Index {
  public static class Unit {
    public String type;
    public int rep;     // index in allUnits
    public int javadoc; // index in javadocs
  }

  public static class RecentUnit {
    public String type;
    public String rep;
    public String javadoc;
  }

  public static final int RECENT_SIZE = 200;
    // TODO(radugrigore): make configurable

  public String[] javadocs;
  public String unitsBuffer; 
    // contains all units' canonical names, concatenated in
    // lexicografic order: AaBbCc...
  public int[] unitsBufferSuffixes; // suffix array for unitsBuffer
  public Unit[] allUnits;
  public RecentUnit[] recentUnits;

  // bracket the result set
  private int left;
  private int right;
  private int needleLen;

  public void needle(String needle) {
    left = 0;
    right = unitsBuffer.length();
    needleLen = 0;
    for (int i = 0; i < needle.length(); ++i)
      appendToNeedle(needle.charAt(i));
  }

  private char filterChar(char c) {
    c = Character.toLowerCase(c);
    if (c == '/') c = '.';
    return c;
  }

  public void appendToNeedle(char c) {
    c = filterChar(c);
    int l, r, m, k;
    l = left - 1;
    r = right;
    while (r - l > 1) {
      m = (l + r) / 2;
      k = unitsBufferSuffixes[m] + needleLen;
      if (k >= unitsBuffer.length() || 
          filterChar(unitsBuffer.charAt(k)) < c)
        l = m;
      else
        r = m;
    }
    left = r;
    l = left - 1;
    r = right;
    while (r - l > 1) {
      m = (l + r) / 2;
      k = unitsBufferSuffixes[m] + needleLen;
      if (filterChar(unitsBuffer.charAt(k)) > c)
        r = m;
      else
        l = m;
    }
    right = r;
    ++needleLen;
  }

  public int resultsCount() {
    return right - left;
  }

  public HTML[] getMoreResults(int maxResults) {
    ArrayList<HTML> r = new ArrayList<HTML>();
    for (int i = left; i < right && r.size() < maxResults; ++i) {
      int j, k;
      j = k = unitsBufferSuffixes[i];
      while (j > 0 && unitsBuffer.charAt(j) != '\n') --j;
      while (k < unitsBuffer.length() && unitsBuffer.charAt(k) != '\n') ++k;
      r.add(new HTML(unitsBuffer.substring(j+1,k) + "<br/>"));
    }
    return r.toArray(new HTML[0]);
  }

  public HTML[] searchRecent(String needle, int maxResults) {
    return null;
  }

  public void use(RecentUnit ru) {
  }
}
