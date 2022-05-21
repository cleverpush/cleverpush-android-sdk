package com.cleverpush.banner.models;


import java.util.HashMap;
import java.util.Map;

public enum BannerAppVersionFilterRelation {
    GreaterThan,
    Equals,
    LessThan,
    Between,
    NotEqual,
    Contains,
    NotContains;

    private static Map<String, BannerAppVersionFilterRelation> appVersionFilter = new HashMap<>();

    static {
        appVersionFilter.put("greaterThan", BannerAppVersionFilterRelation.GreaterThan);
        appVersionFilter.put("equals", BannerAppVersionFilterRelation.Equals);
        appVersionFilter.put("lessThan", BannerAppVersionFilterRelation.LessThan);
        appVersionFilter.put("between", BannerAppVersionFilterRelation.Between);
        appVersionFilter.put("notEqual", BannerAppVersionFilterRelation.NotEqual);
        appVersionFilter.put("contains", BannerAppVersionFilterRelation.Contains);
        appVersionFilter.put("notContains", BannerAppVersionFilterRelation.NotContains);
    }

    public static BannerAppVersionFilterRelation fromString(String raw) {
        if (appVersionFilter.containsKey(raw)) {
            return appVersionFilter.get(raw);
        } else {
            throw new IllegalArgumentException("Unknown banner app version: " + raw);
        }
    }

}
