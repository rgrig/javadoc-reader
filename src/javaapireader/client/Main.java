package javaapireader.client;

import java.util.ArrayList;
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
  private long startSetUrl;
  private final Label timeLabelA = new Label();
  private final Label timeLabelB = new Label();
  private final Label timeLabelC = new Label();
  private final Label timeLabelD = new Label();

  // the result of the last search
  private Index index;
  private Finder packageFinder = new Finder();
  private Finder classFinder = new Finder();

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
    final VerticalPanel statisticsPanel = new VerticalPanel();
    statisticsPanel.add(new HTML("<h3>Statistics</h3>"));
    statisticsPanel.add(timeLabelB);
    statisticsPanel.add(timeLabelA);
    statisticsPanel.add(timeLabelC);
    statisticsPanel.add(timeLabelD);
    leftPanel.add(statisticsPanel); // for release, comment this line
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
        reportMore(classFinder, classesPanel, classesMenuPanel); 
      }
    });
    morePackagesButton.addClickHandler(new ClickHandler() {
      @Override public void onClick(ClickEvent e) {
        reportMore(packageFinder, packagesPanel, packagesMenuPanel);
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
            overviewPanel.clear();
            overviewPanel.add(new HTML(
                "<a href=\"" + index.url() + "/overview-summary.html" +
                "\" target=\"classFrame\">Overview</a>"));
            reportTime("fetching", timeLabelB, startSetUrl, afterFetch);
            Scanner s = new Scanner(response.getText());
            int pCnt = s.nextInt();
            int cCnt = s.nextInt();
            for (int i = 0; i < pCnt; ++i)
              index.addPackage(s.next());
            for (int i = 0; i < cCnt; ++i) {
              String className = s.next();
              boolean isInterface = s.nextBool();
              int packageIdx = s.nextInt();
              index.addClass(className, isInterface, packageIdx);
            }
            classFinder.hay(index.allClasses);
            packageFinder.hay(index.allPackages);
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
    boolean hasDot = false;
    boolean hasSpecial = false;
    for (int i = 0; i < needle.length(); ++i) {
      hasDot |= needle.charAt(i) == '.';
      hasSpecial |= 
          !Character.isLetterOrDigit(needle.charAt(i)) && 
          needle.charAt(i) != '.';
    }
    if (hasSpecial)
      needle = needle.replaceAll("\\.", "\\.").replaceAll("\\*", ".*");
    else if (hasDot)
      needle = ".*" + needle.replaceAll("\\.", "\\\\.") + ".*";
    else
      needle = ".*" + needle + "[^\\.]*";
    classFinder.needle(needle);
    packageFinder.needle(needle);

    classesPanel.clear();
    classesPanel.add(new HTML("<h2>Classes</h2>"));
    classesMenuPanel.add(moreClassesButton);
    packagesPanel.clear();
    packagesPanel.add(new HTML("<h2>Packages</h2>"));
    packagesMenuPanel.add(morePackagesButton);
    reportTime("preparing", timeLabelC, start, System.currentTimeMillis());
    reportMore(classFinder, classesPanel, classesMenuPanel);
    reportMore(packageFinder, packagesPanel, packagesMenuPanel);
  }

  private void reportMore(
      Finder finder, 
      ComplexPanel resultPanel, 
      Panel morePanel
  ) {
    long start = System.currentTimeMillis();
    int toGet = Math.max(INITIAL_RESULT_COUNT, resultPanel.getWidgetCount());
    List<HTML> newResults = new ArrayList<HTML>();
    boolean more = finder.find(toGet, newResults);
    for (HTML h : newResults) resultPanel.add(h);
    if (!more) morePanel.clear();
    reportTime("searching", timeLabelD, start, System.currentTimeMillis());
  }

  private void reportTime(String action, Label target, long a, long b) {
    target.setText(action + " took " + (b-a) + "ms");
  }

  private static boolean contains(String hay, String needle) {
    return hay.toLowerCase().matches(".*" + needle + ".*");
  }
}
