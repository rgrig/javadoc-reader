package javaapireader.client;

import java.util.ArrayList;
import java.util.List;

/** A structure holding the index of a javadoc. */
public final class Index {
  // TODO: How should this be synched with the server?
  public static final int RECENT_SIZE = 100;

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

  private ArrayList<ClassUnit> recentClasses = new ArrayList<ClassUnit>();
  private int recentClassesEnd;

  public Index(String baseUrl) {
    for (int i = 0; i < RECENT_SIZE; ++i) recentClasses.add(null);
    this.baseUrl = baseUrl;
  }

  public String url() {
    return baseUrl;
  }

  public void addClass(String class_, boolean interface_, int packageIdx) {
    allClasses.add(new ClassUnit(
        baseUrl, 
        allPackages.get(packageIdx),
        class_,
        interface_));
  }

  public void addPackage(String package_) {
    allPackages.add(new PackageUnit(baseUrl, package_));
  }

  public void addRecentClass(ClassUnit c) {
    recentClasses.set(recentClassesEnd, c);
    recentClassesEnd = (recentClassesEnd + 1) % RECENT_SIZE;
  }

  public List<ClassUnit> recentClasses() {
    assert false : "todo, order by how often they appear in history";
    return null;
  }

  public List<PackageUnit> recentPackages() {
    assert false : "todo, order by how often they appear in history";
    return null;
  }
}
