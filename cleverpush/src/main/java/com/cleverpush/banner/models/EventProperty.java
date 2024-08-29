package com.cleverpush.banner.models;

import org.json.JSONObject;

public class EventProperty {

  private String property;
  private String value;

  private EventProperty() {
  }

  public String getProperty() {
    return property;
  }

  public String getValue() {
    return value;
  }

  public static EventProperty create(JSONObject json) {
    EventProperty property = new EventProperty();

    if (json != null) {
      property.property = json.optString("property");
      property.value = json.optString("value");
    }

    return property;
  }
}
