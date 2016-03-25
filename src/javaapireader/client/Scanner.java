package javaapireader.client;

/** A very simple version of java.util.Scanner, which is not in GWT. */
public class Scanner {
  private final String s;
  private int pos;
  private String next;

  public Scanner(String s) {
    this.s = s; read();
  }

  public boolean hasNext() {
    return next != null;
  }

  public String next() {
    String r = next;
    read();
    return r;
  }

  // assumes that |next| contains a non-negative integer
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
