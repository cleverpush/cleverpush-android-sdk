package com.cleverpush.banner.models;

import java.util.HashMap;
import java.util.Map;

public enum BannerTriggerType {
	AppOpen,
	Conditions;

    private static Map<String, BannerTriggerType> mapTriggerType = new HashMap<>();
    static {
		mapTriggerType.put("app_open", BannerTriggerType.AppOpen);
		mapTriggerType.put("conditions", BannerTriggerType.Conditions);
    }

    public static BannerTriggerType fromString(String raw) {
        if (mapTriggerType.containsKey(raw)) {
            return mapTriggerType.get(raw);
        } else {
            return AppOpen;
        }
    }
}
