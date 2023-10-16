package com.cleverpush.banner.models;

import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.banner.models.blocks.BannerBlock;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class BannerScreens {
  private String id;
  private List<BannerBlock> blocks;
  private BannerBackground background;

  public String getId() {
    return id;
  }

  public List<BannerBlock> getBlocks() {
    return blocks;
  }

  public BannerBackground getBackground() {
    return background;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setBlocks(List<BannerBlock> blocks) {
    this.blocks = blocks;
  }

  public static BannerScreens create(JSONObject json) throws JSONException {
    BannerScreens bannerScreens = new BannerScreens();

    bannerScreens.id = json.getString("id");
    bannerScreens.blocks = new LinkedList<>();

    JSONArray blockArray = json.getJSONArray("blocks");
    for (int i = 0; i < blockArray.length(); ++i) {
      bannerScreens.blocks.add(BannerBlock.create(blockArray.getJSONObject(i)));
    }

    if (json.has("background")) {
      bannerScreens.background = BannerBackground.create(json.optJSONObject("background"));
    }

    return bannerScreens;
  }
}
