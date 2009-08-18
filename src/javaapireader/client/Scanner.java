package javaapireader.client;

/** 
  A very simple version of java.util.Scanner, which is not in GWT. 
  For speed it makes the assumptions:
   - there is exactly ONE whitespace character after each token
   - integers are nonnegative decimals
   - whitespace means ' ' or '\n'
 */
public class Scanner {
  private String s;
  private int pos;

  public Scanner(String s) { 
    this.s = s;
  }

  public void skip(int cnt) {
    pos += cnt;
  }

  public String next() { 
    char c;
    int oldPos = pos;
    while ((c = s.charAt(pos++)) != ' ' && c != '\n');
    ++pos;
    return s.substring(oldPos, pos - 1);
  }

  public int nextInt() {
    char c;
    int r = 0;
    while ((c = s.charAt(pos++)) != ' ' && c != '\n')
      r = 10 * r + (int) c - (int) '0';
    ++pos;
    return r;
  }
}
