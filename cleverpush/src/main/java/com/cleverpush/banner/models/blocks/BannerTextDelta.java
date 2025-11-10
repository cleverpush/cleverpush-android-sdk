package com.cleverpush.banner.models.blocks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class BannerTextDelta {

  private List<BannerTextOp> ops;

  public List<BannerTextOp> getOps() {
    return ops;
  }

  public static BannerTextDelta create(JSONObject json) throws JSONException {
    BannerTextDelta bannerTextDelta = new BannerTextDelta();
    bannerTextDelta.ops = new LinkedList<>();

    if (json != null) {
      JSONArray conditionsArray = json.getJSONArray("ops");
      for (int i = 0; i < conditionsArray.length(); ++i) {
        bannerTextDelta.ops.add(BannerTextOp.create(conditionsArray.getJSONObject(i)));
      }
    }

    return bannerTextDelta;
  }

}
