package javaapireader.client;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.user.client.ui.HTML;

/** A class or a package. */
public abstract class Unit<T extends Unit> {
  private String lowercaseName;

  // these are informed when the link() is clicked
  private Index index;
  private Finder<T> finder;

  public Unit(Index index, Finder<T> finder) {
    assert index != null;
    this.index = index;
    this.finder = finder;
  }

  /** All units remember their javadoc. */
  public String url() {
    return index.url();
  }

  public Index index() { return index; }
  public Finder finder() { return finder; }

  /** The thing put in the results panel. */
  public abstract HTML link();

  /** The string we match against. */
  public abstract String rep();

  /** The thing used for the link text. */
  public abstract String name();
  
  /** Used for sorting the results. */
  public String lowercaseName() {
    if (lowercaseName == null) lowercaseName = name().toLowerCase();
    return lowercaseName;
  }

  /** Send a message announcing the server that this unit was used. */
  public void touch(String u) {
    String request = "/workingset?uid=" + Main.uid() + 
        "&url=" + url() + "&unit=" + u;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, request);
    try {
      rb.sendRequest(null, new RequestCallback() {
        @Override public void onError(Request r, Throwable e) {}
        @Override public void onResponseReceived(Request request, Response response) {}
      });
    } catch (RequestException e) {
      // ignore (nothing much we can do about it)
    }
  }
}
