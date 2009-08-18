package javaapireader.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

public class Main implements EntryPoint {
  private static String uid; // who am i?
  public static String uid() { return uid; }

  // some UI elements
  private final TextBox findBox = new TextBox();
  private final VerticalPanel resultsPanel = new VerticalPanel();

  // performance-related
  private static final int INITIAL_RESULT_COUNT = 20;
  private static final int TIME_LABELS_COUNT = 10;
  private long startSetUrl;
  private final Label[] timeLabels = new Label[TIME_LABELS_COUNT];

  // the result of the last search
  private Index index;

  /** Entry point. */
  public void onModuleLoad() {
    // set up the layout
    final HorizontalPanel findPanel = new HorizontalPanel();
    findBox.setWidth("230px");
    findPanel.add(findBox);
    final VerticalPanel leftPanel = new VerticalPanel();
    leftPanel.add(findPanel);
    leftPanel.add(resultsPanel);
    final VerticalPanel statisticsPanel = new VerticalPanel();
    statisticsPanel.add(new HTML("<h3>Statistics</h3>"));
    for (int i = 0; i < TIME_LABELS_COUNT; ++i) {
      timeLabels[i] = new Label();
      statisticsPanel.add(timeLabels[i]);
    }
    leftPanel.add(statisticsPanel); // for LIVE version, comment this line
    RootPanel.get().add(leftPanel);

    findBox.setFocus(true);
    findBox.addKeyUpHandler(new KeyUpHandler() {
      @Override public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
          find(findBox.getText());
      }
    });

    loadIndex(
        // TODO(radugrigore): make configurable
        "http://java.sun.com/javase/6/docs/api",
        "http://google-web-toolkit.googlecode.com/svn/javadoc/1.6",
        "http://google-collections.googlecode.com/svn/trunk/javadoc",
        "http://help.eclipse.org/galileo/nftopic/org.eclipse.platform.doc.isv/reference/api",
        "http://bits.netbeans.org/dev/javadoc"
    );
  }

  // ask for an index for the javadoc at |url|
  private void loadIndex(String... javadocs) {
    startSetUrl = System.currentTimeMillis();
    findBox.setText("");
    index = new Index();
    String query = "";
    for (int i = 0; i < javadocs.length; ++i) 
      query += "&url" + i + "=" + javadocs[i];
    getWorkingSet(query);
    getAllUnits(query);
  }

  private void getWorkingSet(String javadocs) {
    uid = Cookies.getCookie("java-api.uid");
    String url = "/workingset?";
    if (uid != null) url += "uid=" + uid;
    url += javadocs;
    RequestBuilder rb = new RequestBuilder(RequestBuilder.GET, url);
    rb.setHeader("Accept-Encoding", "gzip");
    rb.setHeader("User-Agent", "gzip");
    try {
      rb.sendRequest(null, new RequestCallback() {
        @Override public void onError(Request r, Throwable e) {
Window.alert("DBG: http request error: " + e);
        }

        @Override public void onResponseReceived(Request request, Response response) {
          if (response.getStatusCode() == 200) {
            Scanner s = new Scanner(response.getText());
            Cookies.setCookie(
              "java-api.uid", 
              uid = s.next(),
              new Date(System.currentTimeMillis() + 1000l * 60l * 60l * 24l * 365l));
            assert false : "todo: parsing of working set: use different format";
            reportTime("fetching and parsing working set", timeLabels[0], startSetUrl, System.currentTimeMillis());
            find(findBox.getText());
          }
        }
      });
    } catch (RequestException e) {
Window.alert("DBG: RequestException: " + e);
    }
  }

  private void getAllUnits(String javadocs) {
    RequestBuilder rb = new RequestBuilder(
        RequestBuilder.GET,
        "/fetch?" + javadocs);
    rb.setHeader("Accept-Encoding", "gzip");
    rb.setHeader("User-Agent", "gzip");
    try {
      rb.sendRequest(null, new RequestCallback() {
        @Override public void onError(Request r, Throwable e) {
Window.alert("DBG: http request error: " + e);
        }

        @Override public void onResponseReceived(Request request, Response response) {
          if (response.getStatusCode() == 200) {
            long afterFetch = System.currentTimeMillis();
            reportTime("fetching all", timeLabels[1], startSetUrl, afterFetch);
            Scanner s = new Scanner(response.getText());
            assert false : "todo: parsing of index";
            reportTime("parsing", timeLabels[2], afterFetch, System.currentTimeMillis());
          }
        }
      });
    } catch (RequestException e) { }
  }

  private void find(String needle) {
    long start = System.currentTimeMillis();
    resultsPanel.clear();

    if (needle.equals("")) {
      assert false : "todo";
      reportTime("identifying most used", timeLabels[3], start, System.currentTimeMillis());
    } else {
      needle = needle.toLowerCase();
      boolean hasDot = false;
      boolean hasSpecial = false;
      for (int i = 0; i < needle.length(); ++i) {
        hasDot |= needle.charAt(i) == '.';
        hasSpecial |= 
            !Character.isLetterOrDigit(needle.charAt(i)) && 
            needle.charAt(i) != '.';
      }
      if (hasSpecial)
        needle = "^" + needle.replaceAll("\\.", "\\.").replaceAll("\\*", ".*") + "$";
      else if (hasDot)
        needle = needle.replaceAll("\\.", "\\\\.");
      else
        needle = needle + "[^\\.]*$";
      reportTime("preparing", timeLabels[3], start, System.currentTimeMillis());
      assert false : "todo: the actual search";
    }
  }

  private void reportTime(String action, Label target, long a, long b) {
    target.setText(action + " took " + (b-a) + "ms");
  }
}
