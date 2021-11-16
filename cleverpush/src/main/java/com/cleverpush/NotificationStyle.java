package com.cleverpush;

import java.util.HashMap;
import java.util.Map;

public enum NotificationStyle {
    AUTO("AUTO"),
    BIG_TEXT("BIG_TEXT"),
    BIG_PICTURE("BIG_PICTURE"),
    TEXT_WITH_IMAGE("TEXT_WITH_IMAGE");

    private final String code;
    private static final Map<String, NotificationStyle> valuesByCode;

    static {
        valuesByCode = new HashMap<>(values().length);
        for(NotificationStyle value : values()) {
            valuesByCode.put(value.code, value);
        }
    }

    NotificationStyle(String code) {
        this.code = code;
    }

    public static NotificationStyle lookupByCode(String code) {
        return valuesByCode.get(code);
    }

    public String getCode() {
        return code;
    }
}
