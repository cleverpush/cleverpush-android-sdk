package com.cleverpush.util;

import java.util.LinkedList;

public class LimitedSizeQueue<E> extends LinkedList<E> {
  private int capacity;

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  @Override
  public boolean add(E e) {
    while (size() >= capacity) {
      removeFirst();
    }
    return super.add(e);
  }
}
