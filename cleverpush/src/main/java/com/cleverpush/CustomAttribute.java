package com.cleverpush;

public class CustomAttribute {
  private final String id;
  private final String name;

  public CustomAttribute(String id, String name) {
    this.id = id;
    this.name = name;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String toString() {
    return this.getId();
  }
}
