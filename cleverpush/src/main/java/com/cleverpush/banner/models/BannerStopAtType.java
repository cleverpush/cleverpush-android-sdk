package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerStopAtType {
  Forever,
  SpecificTime,
  RelativeToDelivery;

  private static final Map<String, BannerStopAtType> mapStopAtType = new HashMap<>();

  static {
    mapStopAtType.put("forever", BannerStopAtType.Forever);
    mapStopAtType.put("specific_time", BannerStopAtType.SpecificTime);
    mapStopAtType.put("relative_to_delivery", BannerStopAtType.RelativeToDelivery);
  }

  public static BannerStopAtType fromString(String raw) {
    if (!mapStopAtType.containsKey(raw)) {
      return Forever;
    }
    return mapStopAtType.get(raw);
  }
}
