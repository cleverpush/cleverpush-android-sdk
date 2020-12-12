package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerStopAtType {
    Forever,
    SpecificTime;

    private static Map<String, BannerStopAtType> mapStopAtType = new HashMap<>();
    static {
        mapStopAtType.put("forever", BannerStopAtType.Forever);
        mapStopAtType.put("specific_time", BannerStopAtType.SpecificTime);
    }

    public static BannerStopAtType fromString(String raw) {
        if(mapStopAtType.containsKey(raw)) {
            return mapStopAtType.get(raw);
        } else {
            throw new IllegalArgumentException("Unknown banner stop at type: " + raw);
        }
    }
}
