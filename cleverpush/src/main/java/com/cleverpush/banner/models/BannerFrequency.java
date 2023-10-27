package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerFrequency {
  Once,
  Once_Per_Session,
  Every_Trigger;

  private static Map<String, BannerFrequency> mapFrequency = new HashMap<>();

  static {
    mapFrequency.put("once", BannerFrequency.Once);
    mapFrequency.put("once_per_session", BannerFrequency.Once_Per_Session);
    mapFrequency.put("every_trigger", BannerFrequency.Every_Trigger);
  }

  public static BannerFrequency fromString(String raw) {
    if (mapFrequency.containsKey(raw)) {
      return mapFrequency.get(raw);
    } else {
      throw new IllegalArgumentException("Unknown banner frequency: " + raw);
    }
  }
}
