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

  private String baseUrl;

  /*
    These are fairly big so I don't want to clone them. Keeping
    them private is useless. TODO: profile. They are arrays
    because we index in them when we parse what the server sends
    us. (When the server uses indexes instead of full strings to
    refer to packages profiling showed that speed is much better,
    both because less data is transfered and because we don't
    need a hash on the client side.
   */
  public ArrayList<ClassUnit> allClasses = new ArrayList<ClassUnit>();
  public ArrayList<PackageUnit> allPackages = new ArrayList<PackageUnit>();

  private Mru<ClassUnit> recentClasses = new Mru<ClassUnit>(RECENT_SIZE);
  private Mru<PackageUnit> recentPackages = new Mru<PackageUnit>(RECENT_SIZE);

  public Index(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String url() { 
    return baseUrl;
  }

  public void addRecentClass(ClassUnit c) {
    recentClasses.use(c);
  }

  public List<ClassUnit> recentClasses() {
    return recentClasses.top();
  }

  public void addRecentPackage(PackageUnit p) {
    recentPackages.use(p);
  }

  public List<PackageUnit> recentPackages() {
    return recentPackages.top();
  }
}
