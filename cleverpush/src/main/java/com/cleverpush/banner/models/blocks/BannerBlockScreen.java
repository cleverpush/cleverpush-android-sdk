package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerBlockScreen {
    String label;
    String value;


    private BannerBlockScreen() {
    }

    public String getLabel() {
        return label;
    }

    public String getValue() {
        return value;
    }

    public static BannerBlockScreen create(JSONObject json) throws JSONException {
        BannerBlockScreen bannerBlockScreen = new BannerBlockScreen();
        if (json != null) {
            bannerBlockScreen.label = json.getString("label");
            bannerBlockScreen.value = json.getString("value");
        }
        return bannerBlockScreen;
    }


}
