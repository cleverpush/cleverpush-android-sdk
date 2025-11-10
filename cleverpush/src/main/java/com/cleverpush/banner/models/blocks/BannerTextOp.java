package com.cleverpush.banner.models.blocks;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerTextOp {
  private String insert;
  private BannerTextAttributes attributes;

  public String getInsert() {
    return insert;
  }

  public BannerTextAttributes getAttributes() {
    return attributes;
  }

  public static BannerTextOp create(JSONObject json) throws JSONException {
    BannerTextOp bannerTextOp = new BannerTextOp();
    if (json != null) {
      if (json.has("insert")) {
        bannerTextOp.insert = json.optString("insert");
      }

      if (json.has("attributes")) {
        bannerTextOp.attributes = BannerTextAttributes.create(json.optJSONObject("attributes"));
        ;
      }
    }
    return bannerTextOp;
  }
}
