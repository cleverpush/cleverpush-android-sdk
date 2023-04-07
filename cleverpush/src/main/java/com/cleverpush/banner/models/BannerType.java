package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerType {
  Top,
  Bottom,
  Center,
  Full;

  private static Map<String, BannerType> mapType = new HashMap<>();

  static {
    mapType.put("top", BannerType.Top);
    mapType.put("bottom", BannerType.Bottom);
    mapType.put("center", BannerType.Center);
    mapType.put("full", BannerType.Full);
  }

  public static BannerType fromString(String raw) {
    if (mapType.containsKey(raw)) {
      return mapType.get(raw);
    } else {
      return Center;
    }
  }
}
