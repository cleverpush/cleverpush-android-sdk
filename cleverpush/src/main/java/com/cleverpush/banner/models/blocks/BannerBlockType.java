package com.cleverpush.banner.models.blocks;

import java.util.HashMap;
import java.util.Map;

public enum BannerBlockType {
    Text,
    Image,
    Button,
    HTML;

    private static final Map<String, BannerBlockType> typeMap = new HashMap<>();
    static {
        typeMap.put("text", BannerBlockType.Text);
        typeMap.put("image", BannerBlockType.Image);
        typeMap.put("button", BannerBlockType.Button);
        typeMap.put("html", BannerBlockType.HTML);
    }

    public static BannerBlockType fromString(String raw) {
        if (typeMap.containsKey(raw)) {
            return typeMap.get(raw);
        } else {
            throw new IllegalArgumentException("Unknown banner type: " + raw);
        }
    }
}
