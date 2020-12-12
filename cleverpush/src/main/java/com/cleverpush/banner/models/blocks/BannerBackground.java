package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerBackground {
    private String imageUrl;
    private String color;
    private boolean dismiss;

    private BannerBackground() {}

    public String getImageUrl() { return imageUrl; }

    public String getColor() { return color; }

    public boolean dismissOnClick() { return dismiss; }

    public static BannerBackground create(JSONObject json) throws JSONException {
        BannerBackground bg = new BannerBackground();

        bg.imageUrl = json.getString("imageUrl");
        bg.color = json.getString("color");
        bg.dismiss = json.getBoolean("dismiss");

        return bg;
    }
}
