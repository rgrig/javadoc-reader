package javaapireader.client;

import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
  // the current javdoc
  private String baseUrl;

  // some UI elements
  private final TextBox findBox = new TextBox();
  private final Frame classFrame = new Frame();
  private final VerticalPanel overviewPanel = new VerticalPanel();
  private final VerticalPanel classesPanel = new VerticalPanel();
  private final HorizontalPanel classesMenuPanel = new HorizontalPanel();
  private final Button moreClassesButton = new Button("more");
  private final VerticalPanel packagesPanel = new VerticalPanel();
  private final HorizontalPanel packagesMenuPanel = new HorizontalPanel();
  private final Button morePackagesButton = new Button("more");

  // the index for the current baseUrl
  private String[] packages;
  private String[] classes;
  private int[] packageOfClass;
  private boolean[] isInterface;

  // the result of the last search
  private ArrayList<Integer> matchingClasses = new ArrayList<Integer>();
  private ArrayList<Integer> matchingPackages = new ArrayList<Integer>();
  private static final int INITIAL_RESULT_COUNT = 20;
  private int reportedClasses;
  private int reportedPackages;

  // performance-related
  private long startSetUrl;
  private final Label timeLabelA = new Label();
  private final Label timeLabelB = new Label();
  private final Label timeLabelC = new Label();
  private final Label timeLabelD = new Label();

  // a very simple version of java.util.Scanner, which is not in GWT
  public static class Scanner {
    private final String s;
    private int pos;
    private String next;
    public Scanner(String s) { this.s = s; read(); }
    public boolean hasNext() { return next != null; }
    public String next() { String r = next; read(); return r; }

    public int nextInt() {
      int r = 0;
      for (int i = 0; i < next.length(); ++i)
        r = 10 * r + (int) next.charAt(i) - (int) '0';
      read();
      return r;
    }

    public boolean nextBool() {
      boolean r = !"0".equals(next);
      read();
      return r;
    }

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
    // set up the layout
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
    leftPanel.add(overviewPanel);
    leftPanel.add(classesPanel);
    leftPanel.add(classesMenuPanel);
    leftPanel.add(packagesPanel);
    leftPanel.add(packagesMenuPanel);
    leftPanel.add(new HTML("<h3>Statistics</h3>"));
    leftPanel.add(timeLabelB);
    leftPanel.add(timeLabelA);
    leftPanel.add(timeLabelC);
    leftPanel.add(timeLabelD);
    RootPanel.get().add(leftPanel);

    findBox.setFocus(true);

    // set up the event handlers
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
        else if (timeout > 0) {
          timer.cancel();
          timer.schedule(timeout);
        }
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
    moreClassesButton.addClickHandler(new ClickHandler() {
      @Override public void onClick(ClickEvent e) { reportMoreClasses(); }
    });
    morePackagesButton.addClickHandler(new ClickHandler() {
      @Override public void onClick(ClickEvent e) {reportMorePackages(); }
    });

    // initialize content for the default javadoc
    setUrl(urlBox.getText());
  }

  // ask for an index for the javadoc at |url|
  private void setUrl(String url) {
    startSetUrl = System.currentTimeMillis();
    int i;
    for (i = url.length(); --i >= 0 && url.charAt(i) == '/'; );
    baseUrl = url.substring(0, i + 1);
    RequestBuilder rb = new RequestBuilder(
        RequestBuilder.GET,
        "/fetch?url=" + baseUrl);
    rb.setHeader("Accept-Encoding", "gzip");
    rb.setHeader("User-Agent", "gzip");
    try {
      rb.sendRequest(null, new RequestCallback() {
        @Override public void onError(Request r, Throwable e) {}
        @Override public void onResponseReceived(Request request, Response response) {
          if (response.getStatusCode() == 200) {
            long afterFetch = System.currentTimeMillis();
            overviewPanel.clear();
            overviewPanel.add(new HTML(
                "<a href=\"" + baseUrl + "/overview-summary.html" +
                "\" target=\"classFrame\">Overview</a>"));
            reportTime("fetching", timeLabelB, startSetUrl, afterFetch);
            Scanner s = new Scanner(response.getText());
            int pCnt = s.nextInt();
            int cCnt = s.nextInt();
            packages = new String[pCnt];
            classes = new String[cCnt];
            packageOfClass = new int[cCnt];
            isInterface = new boolean[cCnt];
            for (int i = 0; i < pCnt; ++i)
              packages[i] = s.next();
            for (int i = 0; i < cCnt; ++i) {
              classes[i] = s.next();
              isInterface[i] = s.nextBool();
              packageOfClass[i] = s.nextInt();
            }
            reportTime("parsing", timeLabelA, afterFetch, System.currentTimeMillis());
            find(findBox.getText());
          }
        }
      });
    } catch (RequestException e) { }
  }

  private void find(String needle) {
    long start = System.currentTimeMillis();
    needle = needle.toLowerCase();
    matchingClasses.clear();
    for (int i = 0; i < classes.length; ++i)
      if (contains(classes[i], needle)) matchingClasses.add(i);
    matchingPackages.clear();
    for (int i = 0; i < packages.length; ++i) 
      if (contains(packages[i], needle)) matchingPackages.add(i);
    reportTime("searching", timeLabelC, start, System.currentTimeMillis());

    classesPanel.clear();
    classesPanel.add(new HTML("<h2>Classes</h2>"));
    reportedClasses = 0;
    if (matchingClasses.isEmpty())
      classesPanel.add(new Label("none found"));
    classesMenuPanel.add(moreClassesButton);
    packagesPanel.clear();
    packagesPanel.add(new HTML("<h2>Packages</h2>"));
    reportedPackages = 0;
    if (matchingPackages.isEmpty())
      packagesPanel.add(new Label("none found"));
    packagesMenuPanel.add(morePackagesButton);
    reportMoreClasses();
    reportMorePackages();
  }

  // TODO eliminate duplication between reportMoreClasses() and reportMorePackages()
  private void reportMoreClasses() {
    int limit = Math.min(
        matchingClasses.size(),
        Math.max(INITIAL_RESULT_COUNT, 2 * reportedClasses));
    while (reportedClasses < limit) {
      int ci = matchingClasses.get(reportedClasses);
      String c = classes[ci];
      String p = packages[packageOfClass[ci]];
      boolean ii = isInterface[ci];
      classesPanel.add(new HTML(
          (ii? "<i>" : "") +
          "<a href=\"" +
          baseUrl + "/" + p.replace('.', '/') + "/" + c + ".html" +
          "\" target=\"classFrame\">" + 
          c +
          "</a>" + (ii? "</i>" : "") + "&nbsp;in&nbsp;" +
          p +
          "<br>"));
      ++reportedClasses;
    }
    if (reportedClasses == matchingClasses.size())
      classesMenuPanel.clear();
  }

  private void reportMorePackages() {
    int limit = Math.min(
        matchingPackages.size(),
        Math.max(INITIAL_RESULT_COUNT, 2 * reportedPackages));
    while (reportedPackages < limit) {
      String p = packages[matchingPackages.get(reportedPackages)];
      packagesPanel.add(new HTML(
          "<a href=\"" +
          baseUrl + "/" + p.replace('.', '/') + "/package-summary.html" +
          "\" target=\"classFrame\">" +
          p +
          "</a><br>"));
      ++reportedPackages;
    }
    if (reportedPackages == matchingPackages.size())
      packagesMenuPanel.clear();
  }

  private void reportTime(String action, Label target, long a, long b) {
    target.setText(action + " took " + (b-a) + "ms");
  }

  private static boolean contains(String hay, String needle) {
    return hay.toLowerCase().matches(".*" + needle + ".*");
  }
}
