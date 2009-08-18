package javaapireader.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HTML;

/** A structure holding the index of a javadoc. */
public final class Index {
  public static class Unit {
    public String type;
    public Unit parent;
    public int rep;     // index in allUnits
    public int javadoc; // index in javadocs
  }

  public static class RecentUnit {
    public String javadoc;
    public String type;
    public String rep;
  }

  public static final int RECENT_SIZE = 200;
    // TODO(radugrigore): make configurable

  public String[] javadocs;
  public String unitsBuffer; 
    // contains all units' canonical names, concatenated in
    // lexicografic order: AaBbCc...
  public int[] unitsBufferSuffixes; // suffix array for unitsBuffer
  public Unit[] allUnits;
  public Unit[] recentUnits;

  public void needle(String needle) {
  }

  public HTML[] getMoreResults(int maxResults) {
    return null;
  }

  public HTML[] searchRecent(String needle, int maxResults) {
    return null;
  }

  public void use(RecentUnit ru) {
  }
}
