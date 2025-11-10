package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerTextAttributes {

  private boolean italic;
  private boolean bold;
  private boolean underline;
  private boolean strike;

  public boolean isItalic() {
    return italic;
  }

  public boolean isBold() {
    return bold;
  }

  public boolean isUnderline() {
    return underline;
  }

  public boolean isStrike() {
    return strike;
  }

  public static BannerTextAttributes create(JSONObject json) throws JSONException {
    BannerTextAttributes bannerTextAttributes = new BannerTextAttributes();
    if (json != null) {
      if (json.has("italic")) {
        bannerTextAttributes.italic = json.optBoolean("italic");
      }

      if (json.has("bold")) {
        bannerTextAttributes.bold = json.optBoolean("bold");
      }

      if (json.has("underline")) {
        bannerTextAttributes.underline = json.optBoolean("underline");
      }

      if (json.has("strike")) {
        bannerTextAttributes.strike = json.optBoolean("strike");
      }
    }
    return bannerTextAttributes;
  }
}
