package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerDismissType {
  TillDismissed,
  Timeout;

  private static Map<String, BannerDismissType> mapDismissType = new HashMap<>();

  static {
    mapDismissType.put("till_dismissed", BannerDismissType.TillDismissed);
    mapDismissType.put("timeout", BannerDismissType.Timeout);
  }

  public static BannerDismissType fromString(String raw) {
    if (mapDismissType.containsKey(raw)) {
      return mapDismissType.get(raw);
    } else {
      throw new IllegalArgumentException("Unknown banner dismiss type: " + raw);
    }
  }
}
