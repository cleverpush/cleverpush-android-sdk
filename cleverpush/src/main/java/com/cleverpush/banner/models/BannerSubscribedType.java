package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerSubscribedType {
    All,
	Subscribed,
	Unsubscribed;

    private static final Map<String, BannerSubscribedType> mapSubscribedType = new HashMap<>();
    static {
        mapSubscribedType.put("all", BannerSubscribedType.All);
        mapSubscribedType.put("subscribed", BannerSubscribedType.Subscribed);
        mapSubscribedType.put("unsubscribed", BannerSubscribedType.Unsubscribed);
    }

    public static BannerSubscribedType fromString(String raw) {
        if (mapSubscribedType.containsKey(raw)) {
            return mapSubscribedType.get(raw);
        } else {
            return All;
        }
    }
}
