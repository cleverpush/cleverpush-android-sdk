package com.cleverpush.banner.models;

import com.cleverpush.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BannerTargetEvent {
  private String relation;
  private String property;
  private String value;
  private String event;
  private String fromValue;
  private String toValue;
  private List<BannerTriggerConditionEventProperty> eventProperties;

  public String getRelation() {
    return relation;
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getEvent() {
    return event;
  }

  public String getFromValue() {
    return fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public void setEvent(String event) {
    this.event = event;
  }

  public List<BannerTriggerConditionEventProperty> getEventProperties() {
    return eventProperties;
  }

  public static BannerTargetEvent create(JSONObject json) throws JSONException {
    BannerTargetEvent bannerTargetEvent = new BannerTargetEvent();

    if (json.has("relation")) {
      bannerTargetEvent.relation = json.getString("relation");
    }
    if (json.has("property")) {
      bannerTargetEvent.property = json.getString("property");
    }
    if (json.has("value")) {
      bannerTargetEvent.value = json.getString("value");
    }
    if (json.has("event")) {
      bannerTargetEvent.event = json.getString("event");
    }
    if (json.has("fromValue")) {
      bannerTargetEvent.fromValue = json.getString("fromValue");
    }
    if (json.has("toValue")) {
      bannerTargetEvent.toValue = json.getString("toValue");
    }
    bannerTargetEvent.eventProperties = new ArrayList<>();
    JSONArray eventPropertiesArray = json.optJSONArray("eventProperties");
    if (eventPropertiesArray != null) {
      for (int i = 0; i < eventPropertiesArray.length(); ++i) {
        try {
          BannerTriggerConditionEventProperty property =
              BannerTriggerConditionEventProperty.create(eventPropertiesArray.getJSONObject(i));
          bannerTargetEvent.eventProperties.add(property);
        } catch (JSONException e) {
          Logger.e("CleverPush", "Error creating BannerTriggerConditionEventProperty", e);
        }
      }
    }

    return bannerTargetEvent;
  }
}
