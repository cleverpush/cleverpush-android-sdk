package com.cleverpush.banner.models.blocks;

import java.util.HashMap;
import java.util.Map;

public enum Alignment {
  Left,
  Center,
  Right;

  private static Map<String, Alignment> mapAlignment = new HashMap<>();

  static {
    mapAlignment.put("left", Alignment.Left);
    mapAlignment.put("center", Alignment.Center);
    mapAlignment.put("right", Alignment.Right);
  }

  public static Alignment fromString(String raw) {
    if (mapAlignment.containsKey(raw)) {
      return mapAlignment.get(raw);
    } else {
      throw new IllegalArgumentException("Unknown alignment: " + raw);
    }
  }
}
