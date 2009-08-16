package javaapireader.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.Window;

/** A structure holding the index of a javadoc. */
public final class Index {
  public static final int RECENT_SIZE = 200;
    // TODO(radugrigore): make configurable

  private String baseUrl;

  public String unitsBuffer; 
    // contains all units' canonical names, concatenated in
    // lexicografic order: AaBbCc...
  public int[] unitsBufferSuffixes; // suffix array for unitsBuffer
  public Unit[] allUnits;
  public Unit[] recentUnits;

  public Index(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String url() { 
    return baseUrl;
  }
}
