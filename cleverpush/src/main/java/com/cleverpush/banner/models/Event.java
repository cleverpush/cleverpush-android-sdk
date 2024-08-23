package com.cleverpush.banner.models;

import org.json.JSONObject;

public class Event {

  private String id;
  private String name;

  public Event() {
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public static Event create(JSONObject json) {
    Event event = new Event();

    if (json != null) {
      event.id = json.optString("id");
      event.name = json.optString("name");
    }

    return event;
  }
}
