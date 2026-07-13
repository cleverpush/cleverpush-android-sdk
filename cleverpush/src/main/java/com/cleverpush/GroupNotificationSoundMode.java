package com.cleverpush;

import java.util.HashMap;
import java.util.Map;

public enum GroupNotificationSoundMode {

  ALL_NOTIFICATIONS("ALL_NOTIFICATIONS"),
  FIRST_IN_GROUP_ONLY("FIRST_IN_GROUP_ONLY");

  private static final Map<String, GroupNotificationSoundMode> valuesByCode;

  static {
    valuesByCode = new HashMap<>(values().length);
    for (GroupNotificationSoundMode value : values()) {
      valuesByCode.put(value.code, value);
    }
  }

  private final String code;

  GroupNotificationSoundMode(String code) {
    this.code = code;
  }

  public static GroupNotificationSoundMode lookupByCode(String code) {
    return valuesByCode.get(code);
  }

  public String getCode() {
    return code;
  }
}
