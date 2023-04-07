package com.cleverpush.banner.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class BannerTrigger {
  private List<BannerTriggerCondition> conditions;

  private BannerTrigger() {
  }

  public List<BannerTriggerCondition> getConditions() {
    return conditions;
  }

  public static BannerTrigger create(JSONObject json) throws JSONException {
    BannerTrigger trigger = new BannerTrigger();

    trigger.conditions = new LinkedList<>();

    if (json != null) {
      JSONArray conditionsArray = json.getJSONArray("conditions");
      for (int i = 0; i < conditionsArray.length(); ++i) {
        trigger.conditions.add(BannerTriggerCondition.create(conditionsArray.getJSONObject(i)));
      }
    }

    return trigger;
  }
}
