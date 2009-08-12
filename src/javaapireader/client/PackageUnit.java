package javaapireader.client;

import com.google.gwt.user.client.ui.HTML;

public class PackageUnit implements Unit {
  private String baseUrl;
  private String package_;

  public PackageUnit(String baseUrl, String package_) {
    assert package_ != null;
    assert baseUrl != null;
    this.baseUrl = baseUrl;
    this.package_ = package_;
  }

  private HTML link;
  @Override public HTML link() {
    if (link == null) {
      link = new HTML(
          "<a href=\"" +
          packageUrlBase() + "/package-summary.html" +
          "\" target=\"classFrame\">" +
          package_ +
          "</a><br>");
    }
    return link;
  }

  private String packageUrlBase;
  public String packageUrlBase() {
    if (packageUrlBase == null)
      packageUrlBase = baseUrl + "/" + package_.replace('.', '/');
    return packageUrlBase;
  }

  public String name() {
    return package_;
  }

  private String rep;
  @Override public String rep() {
    if (rep == null) rep = package_.toLowerCase();
    return rep;
  }
}
