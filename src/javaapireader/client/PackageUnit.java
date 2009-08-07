package javaapireader.client;

public class PackageUnit implements Unit {
  private String package_;

  public PackageUnit(String package_) {
    assert package_ != null;
    this.package_ = package_;
  }

  private Hyperlink link;
  @Override public Hyperlink link() {
    if (link == null) {
      // TODO
    }
    return link;
  }

  @Override public String rep() {
    return package_;
  }
}
