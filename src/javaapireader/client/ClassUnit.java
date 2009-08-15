package javaapireader.client;

import java.util.HashMap;

import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

public class ClassUnit extends Unit<ClassUnit> {
  private PackageUnit package_;
  private String class_;
  private boolean isInterface;

  public ClassUnit(
      PackageUnit package_, 
      String class_, 
      boolean isInterface,
      Index index,
      Finder<ClassUnit> finder
  ) {
    super(index, finder);
    assert package_ != null;
    assert class_ != null;
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
      link.addClickHandler(new ClickHandler() {
        @Override public void onClick(ClickEvent event) {
          touch(
              (isInterface? "1" : "0") +
              package_.name() + "/" +
              class_);
          index().addRecentClass(ClassUnit.this);
          finder().hay(index().recentClasses());
        }
      });
    }
    return link;
  }

  @Override public String name() {
    return class_;
  }

  private String rep;
  @Override public String rep() {
    if (rep == null) rep = package_.rep() + "." + class_.toLowerCase();
    return rep;
  }

  @Override public int hashCode() {
    return url().hashCode() ^ package_.hashCode() ^ class_.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (!(o instanceof ClassUnit)) return false;
    ClassUnit cu = (ClassUnit) o;
    return 
        url().equals(cu.url()) && 
        package_.equals(cu.package_) &&
        class_.equals(cu.class_);
  }
}

