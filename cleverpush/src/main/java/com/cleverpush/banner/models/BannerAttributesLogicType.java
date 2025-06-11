package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerAttributesLogicType {
    And,
    Or;

    private static Map<String, BannerAttributesLogicType> mapAttributesLogicType = new HashMap<>();

    static {
        mapAttributesLogicType.put("and", BannerAttributesLogicType.And);
        mapAttributesLogicType.put("or", BannerAttributesLogicType.Or);
    }

    public static BannerAttributesLogicType fromString(String raw) {
        if (mapAttributesLogicType.containsKey(raw)) {
            return mapAttributesLogicType.get(raw);
        } else {
            throw new IllegalArgumentException("Unknown banner attribute logic type: " + raw);
        }
    }
}
