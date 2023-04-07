package com.cleverpush.banner.models;

import org.json.JSONObject;

public class BannerTriggerConditionEventProperty {
  private String property;
  private String relation;
  private String value;

  private BannerTriggerConditionEventProperty() {
  }

  public String getProperty() {
    return property;
  }

  public String getRelation() {
    return relation;
  }

  public String getValue() {
    return value;
  }

  public static BannerTriggerConditionEventProperty create(JSONObject json) {
    BannerTriggerConditionEventProperty property = new BannerTriggerConditionEventProperty();

    if (json != null) {
      property.property = json.optString("property");
      property.relation = json.optString("relation");
      property.value = json.optString("value");
    }

    return property;
  }
}
