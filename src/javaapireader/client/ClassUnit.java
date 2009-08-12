package javaapireader.client;

import java.util.HashMap;

import com.google.gwt.user.client.ui.HTML;

public class ClassUnit implements Unit {
  private String baseUrl;
  private PackageUnit package_;
  private String class_;
  private boolean isInterface;

  public ClassUnit(
      String baseUrl, 
      PackageUnit package_, 
      String class_, 
      boolean isInterface
  ) {
    assert baseUrl != null;
    assert package_ != null;
    assert class_ != null;
    this.baseUrl = baseUrl;
    this.package_ = package_;
    this.class_ = class_;
    this.isInterface = isInterface;
  }

  private HTML link;
  @Override public HTML link() {
    if (link == null) {
      link = new HTML(
          (isInterface? "<i>" : "") +
          "<a href=\"" +
          package_.packageUrlBase() + "/" + class_ + ".html" +
          "\" target=\"classFrame\">" + 
          class_ +
          "</a>" + (isInterface? "</i>" : "") + "&nbsp;in&nbsp;" +
          package_.name() +
          "<br>");
    }
    return link;
  }

  private String rep;
  @Override public String rep() {
    if (rep == null) rep = package_.rep() + "." + class_.toLowerCase();
    return rep;
  }
}

