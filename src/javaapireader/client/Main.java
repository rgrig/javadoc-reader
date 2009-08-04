package javaapireader.client;

import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

public class Main implements EntryPoint {
  private String baseUrl;
  private Set<String> packages = new TreeSet<String>();
  private Map<String,String> classes = new TreeMap<String,String>();
  private final TextBox findBox = new TextBox();
  private final Frame classFrame = new Frame();
  private final VerticalPanel searchResultsPanel = new VerticalPanel();
  private final Label loadTimeLabel = new Label();
  private final Label fTimeLabel = new Label();
  private final Label searchTimeLabel = new Label();

  // performance-related
  private static final int MAX_RESULTS = 50;
  private long startSetUrl;

  public static class Scanner {
    private final String s;
    private int pos;
    private String next;
    public Scanner(String s) { this.s = s; read(); }
    public boolean hasNext() { return next != null; }
    public String next() { String r = next; read(); return r; }

    public void read() {
      while (pos < s.length() && Character.isSpace(s.charAt(pos))) pos++;
      if (pos == s.length()) { next = null; return; }
      int end;
      for (end = pos; end < s.length() && !Character.isSpace(s.charAt(end)); ++end);
      next = s.substring(pos, end);
      pos = end;
    }
  }

  /** Entry point. */
  public void onModuleLoad() {
    final TextBox urlBox = new TextBox();
    urlBox.setText("http://java.sun.com/javase/6/docs/api/");
    final Button urlButton = new Button("set");
    final HorizontalPanel urlPanel = new HorizontalPanel();
    urlPanel.add(urlBox);
    urlPanel.add(urlButton);
    final HorizontalPanel findPanel = new HorizontalPanel();
    findBox.setWidth("230px");
    findPanel.add(findBox);
    final VerticalPanel leftPanel = new VerticalPanel();
    leftPanel.add(urlPanel);
    leftPanel.add(findPanel);
    leftPanel.add(searchResultsPanel);
    leftPanel.add(new HTML("<h3>Statistics</h3>"));
    leftPanel.add(fTimeLabel);
    leftPanel.add(loadTimeLabel);
    leftPanel.add(searchTimeLabel);
    RootPanel.get().add(leftPanel);

    findBox.setFocus(true);

    // Event handlers
    abstract class BoxButtonHandler implements ClickHandler, KeyUpHandler {
      private String lastValue;
      final private TextBox box;
      final private int timeout;
      final private Timer timer = new Timer() {
        @Override public void run() {
          guardedGo();
        }
      };
      public BoxButtonHandler(TextBox box, Button button, int timeout) {
        box.addKeyUpHandler(this);
        if (button != null) button.addClickHandler(this);
        this.box = box;
        this.timeout = timeout;
      }
      @Override public void onClick(ClickEvent event) { callGo(); }
      @Override public void onKeyUp(KeyUpEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) callGo();
        else if (timeout > 0) timer.schedule(timeout);
      }
      private void guardedGo() {
        if (box.getText().equals(lastValue)) return;
        callGo();
      }
      private void callGo() { go(lastValue = box.getText()); }
      public abstract void go(String s);
    }
    BoxButtonHandler findHandler = new BoxButtonHandler(findBox, null, 200) {
      @Override public void go(String s) { find(s); }
    };
    BoxButtonHandler setUrlHandler = new BoxButtonHandler(urlBox, urlButton, 0) {
      @Override public void go(String s) { setUrl(s); }
    };

    setUrl(urlBox.getText());
  }

  private void setUrl(String url) {
    startSetUrl = System.currentTimeMillis();
    searchResultsPanel.add(new HTML("<p>loading&hellip;</p>"));
    packages.clear();
    classes.clear();
    int i;
    for (i = url.length(); --i >= 0 && url.charAt(i) == '/'; );
    baseUrl = url.substring(0, i + 1);
    RequestBuilder rb = new RequestBuilder(
        RequestBuilder.GET,
        "/fetch?url=" + baseUrl);
    try {
      rb.sendRequest(null, new RequestCallback() {
        @Override public void onError(Request r, Throwable e) {}
        @Override public void onResponseReceived(Request request, Response response) {
          if (response.getStatusCode() == 200) {
            reportTime("fetching", fTimeLabel, startSetUrl, System.currentTimeMillis());
            Scanner s = new Scanner(response.getText());
            while (s.hasNext()) {
              String p = s.next();
              String c = s.next();
              packages.add(p);
              classes.put(c, p);
            }
            find(findBox.getText());
            reportTime("loading", loadTimeLabel, startSetUrl, System.currentTimeMillis());
          }
        }
      });
    } catch (RequestException e) { }
  }

  private void find(String needle) {
    long start = System.currentTimeMillis();
    searchResultsPanel.clear();
    addResult("overview-summary.html", "Overview");
    searchResultsPanel.add(new HTML("<h2>Classes</h2>"));
    int results = 0;
    for (Map.Entry<String, String> e : classes.entrySet()) {
      if (contains(e.getKey(), needle)) {
        if (++results == MAX_RESULTS+1) {
          searchResultsPanel.add(new Label("... and others"));
          break;
        }
        addResult(e.getValue().replace('.','/') + "/" + e.getKey() + ".html", e.getKey());
      }
    }
    if (results == 0) searchResultsPanel.add(new Label("none found"));
    searchResultsPanel.add(new HTML("<h2>Packages</h2>"));
    results = 0;
    for (String s : packages) {
      if (contains(s, needle)) {
        if (++results == MAX_RESULTS+1) {
          searchResultsPanel.add(new Label("... and others"));
          break;
        }
        addResult(s.replace('.', '/') + "/package-summary.html", s);
      }
    }
    if (results == 0) searchResultsPanel.add(new Label("none found"));
    reportTime("search", searchTimeLabel, start, System.currentTimeMillis());
  }

  private void reportTime(String action, Label target, long a, long b) {
    target.setText(action + " took " + (b-a) + "ms");
  }

  private void addResult(String link, String label) {
    searchResultsPanel.add(new HTML("<a href=\"" + baseUrl + "/" + link +
        "\" + target=\"classFrame\">" + label + "</a><br>"));
  }

  private static boolean contains(String hay, String needle) {
    return hay.toLowerCase().matches(".*" + needle.toLowerCase() + ".*");
  }
}
