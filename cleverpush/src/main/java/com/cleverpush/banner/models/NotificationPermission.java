package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum NotificationPermission {
  All,
  WithPermission,
  WithoutPermission;

  private static final Map<String, NotificationPermission> mapNotificationPermission = new HashMap<>();

  static {
    mapNotificationPermission.put("all", NotificationPermission.All);
    mapNotificationPermission.put("withPermission", NotificationPermission.WithPermission);
    mapNotificationPermission.put("withoutPermission", NotificationPermission.WithoutPermission);
  }

  public static NotificationPermission fromString(String raw) {
    if (mapNotificationPermission.containsKey(raw)) {
      return mapNotificationPermission.get(raw);
    } else {
      return All;
    }
  }
}
