package javaapireader.client;

import java.util.HashMap;

public class ClassUnit implements Unit {
  private PackageUnit package_;
  private String class_;
  private boolean isInterface;

  public ClassUnit(PackageUnit package_, String class_, boolean isInterface) {
    this.package_ = package_;
    this.class_ = class_;
    this.isInterface = isInterface;
  }

  private Hyperlink link;
  @Override public Hyperlink link() {
    if (link == null) {
      // TODO
    }
    return link;
  }

  private String rep;
  @Override public String rep() {
    if (rep == null) rep = package_.rep() + "." + class_;
    return rep;
  }
}

