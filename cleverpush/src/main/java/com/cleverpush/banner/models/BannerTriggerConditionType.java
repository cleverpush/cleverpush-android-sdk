package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerTriggerConditionType {
  Event,
  Sessions,
  Duration,
  Unsubscribe;

  private static Map<String, BannerTriggerConditionType> mapTriggerType = new HashMap<>();

  static {
    mapTriggerType.put("event", BannerTriggerConditionType.Event);
    mapTriggerType.put("sessions", BannerTriggerConditionType.Sessions);
    mapTriggerType.put("duration", BannerTriggerConditionType.Duration);
    mapTriggerType.put("unsubscribe", BannerTriggerConditionType.Unsubscribe);
  }

  public static BannerTriggerConditionType fromString(String raw) {
    if (mapTriggerType.containsKey(raw)) {
      return mapTriggerType.get(raw);
    } else {
      throw new IllegalArgumentException("Unknown banner trigger condition type: " + raw);
    }
  }
}
