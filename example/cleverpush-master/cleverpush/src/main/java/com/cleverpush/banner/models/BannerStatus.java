package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerStatus {
    Published,
    Draft;

    private static final Map<String, BannerStatus> mapStatus = new HashMap<>();

    static {
        mapStatus.put("published", BannerStatus.Published);
        mapStatus.put("draft", BannerStatus.Draft);
    }

    public static BannerStatus fromString(String raw) {
        if(mapStatus.containsKey(raw)) {
            return mapStatus.get(raw);
        } else {
            throw new IllegalArgumentException("Unknown banner status: " + raw);
        }
    }
}
