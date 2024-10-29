package com.cleverpush.banner.models;

import static com.cleverpush.Constants.LOG_TAG;

import com.cleverpush.Constants;
import com.cleverpush.util.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BannerTriggerCondition {
  private BannerTriggerConditionType type;
  private String event;
  private List<BannerTriggerConditionEventProperty> eventProperties;
  private String relation;
  private int sessions;
  private int seconds;
  private String value;
  private String deepLinkUrl;
  private int days;

  private BannerTriggerCondition() {
  }

  public BannerTriggerConditionType getType() {
    return type;
  }

  public String getEvent() {
    return event;
  }

  public List<BannerTriggerConditionEventProperty> getEventProperties() {
    return eventProperties;
  }

  public int getSessions() {
    return sessions;
  }

  public int getSeconds() {
    return seconds;
  }

  public String getRelation() {
    return relation;
  }

  public String getValue() {
    return value;
  }

  public String getDeepLinkUrl() {
    return deepLinkUrl;
  }

  public int getDays() {
    return days;
  }

  public static BannerTriggerCondition create(JSONObject json) {
    BannerTriggerCondition condition = new BannerTriggerCondition();

    if (json != null) {
      condition.type = BannerTriggerConditionType.fromString(json.optString("type"));
      condition.event = json.optString("event");
      condition.sessions = json.optInt("sessions");
      condition.seconds = json.optInt("seconds");
      condition.relation = json.optString("operator");
      condition.value = json.optString("value");
      condition.deepLinkUrl = json.optString("deepLinkUrl");
      condition.days = json.optInt("days");

      if (condition.type.equals(BannerTriggerConditionType.Unsubscribe)) {
        condition.type = BannerTriggerConditionType.Event;
        condition.event = Constants.CLEVERPUSH_APP_BANNER_UNSUBSCRIBE_EVENT;
      }

      condition.eventProperties = new ArrayList<>();
      JSONArray eventPropertiesArray = json.optJSONArray("eventProperties");
      if (eventPropertiesArray != null) {
        for (int i = 0; i < eventPropertiesArray.length(); ++i) {
          try {
            BannerTriggerConditionEventProperty property =
                BannerTriggerConditionEventProperty.create(eventPropertiesArray.getJSONObject(i));
            condition.eventProperties.add(property);
          } catch (JSONException e) {
            Logger.e(LOG_TAG, "Error creating BannerTriggerConditionEventProperty", e);
          }
        }
      }
    }

    return condition;
  }
}
