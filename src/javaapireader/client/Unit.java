package javaapireader.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.ui.HTML;

/** 
  Units are the things we search for: classes, interfaces, 
  packages, etc.
 */
public class Unit {
  public static enum Type {
    ANNOTATION,
    CLASS,
    ENUM,
    INTERFACE,
    PACKAGE
  }

  private Type type;
  private Unit parent;
    // null means no parent; the parent of a class is its package
    // (what should be the parent of a nested class? the parent of
    // a package?)
  private int start; 
    // index in buffer that points to the start of the representation
    // of this unit

  /** The thing put in the results panel. */
  public HTML link() {
    assert false : "todo";
    return null;
  }
}
