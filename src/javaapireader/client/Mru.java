package javaapireader.client;

import java.util.*;

/**
  Keeps a queue of most recently used items. You can ask for them
  ordered by how many times they appear in the queue.
 */
public class Mru<T> {
  private ArrayList<T> queue = new ArrayList<T>();
  private int start;
  private Map<T, Integer> counts = new HashMap<T, Integer>();

  public Mru(int size) {
    for (int i = 0; i < size; ++i) queue.add(null);
  }

  public void use(T e) {
    dec(queue.get(start));
    inc(e);
    queue.set(start, e);
    start = (start + 1) % queue.size();
  }

  public List<T> top() {
    ArrayList<T> result = new ArrayList<T>();
    for (Map.Entry<T, Integer> e : counts.entrySet())
      if (e.getValue() > 0) result.add(e.getKey());
    Collections.sort(result, new Comparator<T>() {
      @Override public int compare(T a, T b) {
        return getCnt(b) - getCnt(a);
      }
    });
    return result;
  }

  private int getCnt(T e) {
    Integer x = counts.get(e);
    if (x == null) x = 0;
    return x;
  }

  private void inc(T e) {
    counts.put(e, getCnt(e) + 1);
  }

  private void dec(T e) {
    counts.put(e, getCnt(e) - 1);
  }
}
