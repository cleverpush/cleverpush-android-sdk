package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum PlatformType {

  Android,
  iOS;

  private static final Map<String, PlatformType> platformType = new HashMap<>();

  static {
    platformType.put("android", PlatformType.Android);
    platformType.put("ios", PlatformType.iOS);
  }

  public static PlatformType fromString(String raw) {
    if (platformType.containsKey(raw)) {
      return platformType.get(raw);
    }
    return null;
  }
}
