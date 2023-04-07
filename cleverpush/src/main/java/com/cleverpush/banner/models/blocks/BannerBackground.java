package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerBackground {
  private String imageUrl;
  private String darkImageUrl;
  private String color;
  private String darkColor;
  private boolean dismiss;

  private BannerBackground() {
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getColor() {
    return color;
  }

  public String getDarkImageUrl() {
    return darkImageUrl;
  }

  public String getDarkColor() {
    return darkColor;
  }

  public boolean dismissOnClick() {
    return dismiss;
  }

  public static BannerBackground create(JSONObject json) throws JSONException {
    BannerBackground bg = new BannerBackground();

    if (json.has("imageUrl")) {
      bg.imageUrl = json.optString("imageUrl");
    }
    if (json.has("darkImageUrl") && !json.optString("darkImageUrl").isEmpty()) {
      bg.darkImageUrl = json.optString("darkImageUrl");
    }

    if (json.has("color")) {
      bg.color = json.optString("color");
    }
    if (json.has("darkColor") && !json.optString("darkColor").isEmpty()) {
      bg.darkColor = json.optString("darkColor");
    }

    if (json.has("dismiss")) {
      bg.dismiss = json.optBoolean("dismiss");
    }

    return bg;
  }
}
