package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerBackground {
    private String imageUrl;
    private String color;
    private boolean dismiss;

    private BannerBackground() {
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getColor() {
        return color;
    }

    public boolean dismissOnClick() {
        return dismiss;
    }

    public static BannerBackground create(JSONObject json) throws JSONException {
        BannerBackground bg = new BannerBackground();

        if (json.has("imageUrl")) {
            bg.imageUrl = json.optString("imageUrl");
        }

        if (json.has("color")) {
            bg.color = json.optString("color");
        }

        if (json.has("dismiss")) {
            bg.dismiss = json.optBoolean("dismiss");
        }

        return bg;
    }
}
