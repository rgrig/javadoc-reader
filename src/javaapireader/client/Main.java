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
import com.google.gwt.http.client.*;
import com.google.gwt.user.client.*;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;

public class Main implements EntryPoint {
  private static String uid; // who am i?
  public static String uid() { return uid; }

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

  // performance-related
  private static final int INITIAL_RESULT_COUNT = 20;
  private static final int TIME_LABELS_COUNT = 10;
  private long startSetUrl;
  private final Label[] timeLabels = new Label[TIME_LABELS_COUNT];

  // the result of the last search
  private Index index;
  private Finder<PackageUnit> workingSetPackageFinder =
      new Finder<PackageUnit>();
  private Finder<ClassUnit> workingSetClassFinder = new Finder<ClassUnit>();
  private Finder<PackageUnit> packageFinder = new Finder<PackageUnit>();
  private Finder<ClassUnit> classFinder = new Finder<ClassUnit>();

  /** Entry point. */
  public void onModuleLoad() {
    // set up the layout
    final TextBox urlBox = new TextBox();
    urlBox.setText("http://download.java.net/jdk9/docs/api/");
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
    final VerticalPanel statisticsPanel = new VerticalPanel();
    statisticsPanel.add(new HTML("<h3>Statistics</h3>"));
    for (int i = 0; i < TIME_LABELS_COUNT; ++i) {
      timeLabels[i] = new Label();
      statisticsPanel.add(timeLabels[i]);
    }
    leftPanel.add(statisticsPanel); // for LIVE version, comment this line
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
      @Override public void onClick(ClickEvent e) {
        reportMore(classFinder, classesPanel, classesMenuPanel, timeLabels[3]);
      }
    });
    morePackagesButton.addClickHandler(new ClickHandler() {
      @Override public void onClick(ClickEvent e) {
        reportMore(packageFinder, packagesPanel, packagesMenuPanel, timeLabels[4]);
      }
    });

    // initialize content for the default javadoc
    setUrl(urlBox.getText());
  }

  // ask for an index for the javadoc at |url|
  private void setUrl(String url) {
    startSetUrl = System.currentTimeMillis();
    url = URL.encode(url);
    int i;
    if (url.endsWith("/") || url.endsWith(".html")) {
      i = url.lastIndexOf('/');
      if (i == -1) {
Window.alert("DBG: Bad url " + url);
        return;
      }
      url = url.substring(0, i);
    }
    index = new Index(url);
    overviewPanel.clear();
    overviewPanel.add(new HTML(
        "<a href=\"" + index.url() + "/overview-summary.html" +
        "\" target=\"classFrame\">Overview</a>"));
    getWorkingSet();
    getAllUnits();
  }

  private void getWorkingSet() {
    uid = Cookies.getCookie("java-api.uid");
    String url = "/workingset?";
    if (uid != null) url += "uid=" + uid + "&";
    url += "url=" + index.url();
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
            int cCnt = s.nextInt();
            for (int i = 0; i < cCnt; ++i) {
              boolean isInterface = false;
              String unitName = s.next();
              switch (unitName.charAt(0)) {
                case '1': // an interface
                  isInterface = true;
                case '0': // a class
                  int split = unitName.indexOf('/');
                  PackageUnit p = new PackageUnit(
                      unitName.substring(1, split),
                      index,
                      null); // this should never be clicked
                  index.addRecentClass(new ClassUnit(
                      p,
                      unitName.substring(split+1, unitName.length()),
                      isInterface,
                      index,
                      workingSetClassFinder));
                  break;
                case '2': // a package
                  index.addRecentPackage(new PackageUnit(
                      unitName.substring(1, unitName.length()),
                      index,
                      workingSetPackageFinder));
                  break;
                default: // huh?
Window.alert("DBG: error parsing unit: " + unitName);
              }
            }
            workingSetClassFinder.hay(index.recentClasses());
            workingSetPackageFinder.hay(index.recentPackages());
            reportTime("fetching and parsing working set", timeLabels[0], startSetUrl, System.currentTimeMillis());
            find(findBox.getText());
          }
        }
      });
    } catch (RequestException e) {
Window.alert("DBG: RequestException: " + e);
    }
  }

  private void getAllUnits() {
    RequestBuilder rb = new RequestBuilder(
        RequestBuilder.GET,
        "/fetch?url=" + index.url());
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
            int pCnt = s.nextInt();
            int cCnt = s.nextInt();
            for (int i = 0; i < pCnt; ++i) {
              index.allPackages.add(new PackageUnit(
                  s.next(), index, workingSetPackageFinder));
            }
            for (int i = 0; i < cCnt; ++i) {
              String className = s.next();
              boolean isInterface = s.nextBool();
              int packageIdx = s.nextInt();
              index.allClasses.add(new ClassUnit(
                  index.allPackages.get(packageIdx),
                  className,
                  isInterface,
                  index,
                  workingSetClassFinder));
            }
            classFinder.hay(index.allClasses);
            packageFinder.hay(index.allPackages);
            reportTime("parsing", timeLabels[2], afterFetch, System.currentTimeMillis());
            find(findBox.getText());
          }
        }
      });
    } catch (RequestException e) { }
  }

  private <T extends Unit> void displayWorkingSetTop(
      Finder<T> finder,
      Panel panel
  ) {
    List<T> units = finder.hay();
    int toDisplay = Math.min(units.size(), INITIAL_RESULT_COUNT);
    List<T> topUnits = new ArrayList<T>();
    Iterator<T> it = units.iterator();
    while (topUnits.size() < toDisplay)
      topUnits.add(it.next());
    Collections.sort(topUnits, new Comparator<T>() {
      @Override public int compare(T a, T b) {
        return a.lowercaseName().compareTo(b.lowercaseName());
      }
    });
    for (T u : topUnits) panel.add(u.link());
  }

  private void find(String needle) {
    long start = System.currentTimeMillis();
    classesPanel.clear();
    classesPanel.add(new HTML("<h2>Classes</h2>"));
    packagesPanel.clear();
    packagesPanel.add(new HTML("<h2>Packages</h2>"));

    if (needle.equals("")) {
      classesMenuPanel.clear();
      packagesMenuPanel.clear();
      displayWorkingSetTop(workingSetClassFinder, classesPanel);
      displayWorkingSetTop(workingSetPackageFinder, packagesPanel);
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
      classFinder.needle(needle);
      packageFinder.needle(needle);

      classesMenuPanel.add(moreClassesButton);
      packagesMenuPanel.add(morePackagesButton);
      reportTime("preparing", timeLabels[3], start, System.currentTimeMillis());
      reportMore(classFinder, classesPanel, classesMenuPanel, timeLabels[4]);
      reportMore(packageFinder, packagesPanel, packagesMenuPanel, timeLabels[5]);
    }
  }

  private void reportMore(
      Finder finder,
      ComplexPanel resultPanel,
      Panel morePanel,
      Label timeLabel
  ) {
    long start = System.currentTimeMillis();
    int toGet = Math.max(INITIAL_RESULT_COUNT, resultPanel.getWidgetCount());
    List<HTML> newResults = new ArrayList<HTML>();
    boolean more = finder.find(toGet, newResults);
    for (HTML h : newResults) resultPanel.add(h);
    if (!more) morePanel.clear();
    reportTime("searching", timeLabel, start, System.currentTimeMillis());
  }

  private void reportTime(String action, Label target, long a, long b) {
    target.setText(action + " took " + (b-a) + "ms");
  }

  private static boolean contains(String hay, String needle) {
    return hay.toLowerCase().matches(".*" + needle + ".*");
  }
}
