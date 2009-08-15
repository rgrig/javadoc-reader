package javaapireader.client;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class PackageUnit extends Unit<PackageUnit> {
  private String package_;

  public PackageUnit(
      String package_,
      Index index,
      Finder<PackageUnit> finder
  ) {
    super(index, finder);
    assert package_ != null;
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
      link.addClickHandler(new ClickHandler() {
        @Override public void onClick(ClickEvent event) {
          touch("2" + package_);
          index().addRecentPackage(PackageUnit.this);
          finder().hay(index().recentPackages());
        }
      });
    }
    return link;
  }

  private String packageUrlBase;
  public String packageUrlBase() {
    if (packageUrlBase == null)
      packageUrlBase = url() + "/" + package_.replace('.', '/');
    return packageUrlBase;
  }

  @Override public String name() {
    return package_;
  }

  private String rep;
  @Override public String rep() {
    if (rep == null) rep = package_.toLowerCase();
    return rep;
  }

  @Override public int hashCode() {
    return url().hashCode() ^ package_.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof PackageUnit)) return false;
    PackageUnit pu = (PackageUnit) o;
    return url().equals(pu.url()) && package_.equals(pu.package_);
  }
}
