package com.cleverpush.banner.models.blocks;

import com.cleverpush.banner.models.BannerAction;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public class BannerImageBlock extends BannerBlock {
  private String imageUrl;
  private String darkImageUrl;
  private int scale;
  private boolean dismiss;
  private BannerAction action;
  private List<BannerBlockScreen> blockScreens;
  private String id;
  private int imageWidth;
  private int imageHeight;

  private BannerImageBlock() {
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getDarkImageUrl() {
    return darkImageUrl;
  }

  public float getScale() {
    return scale;
  }

  public boolean dismissOnClick() {
    return dismiss;
  }

  public BannerAction getAction() {
    return action;
  }

  public List<BannerBlockScreen> getBlocks() {
    return blockScreens;
  }

  public String getId() {
    return id;
  }

  public int getImageWidth() {
    return imageWidth;
  }

  public int getImageHeight() {
    return imageHeight;
  }

  public static BannerImageBlock createImageBlock(JSONObject json) throws JSONException {
    BannerImageBlock imageBlock = new BannerImageBlock();

    imageBlock.type = BannerBlockType.Image;
    if (json.optString("imageUrl") != null) {
      imageBlock.imageUrl = json.optString("imageUrl");
    }

    if (json.has("darkImageUrl") && !json.optString("darkImageUrl").isEmpty()) {
      imageBlock.darkImageUrl = json.optString("darkImageUrl");
    }

    imageBlock.scale = json.optInt("scale");

    imageBlock.dismiss = json.optBoolean("dismiss");

    if (json.has("action")) {
      imageBlock.action = BannerAction.create(json.getJSONObject("action"));
    }

    imageBlock.blockScreens = new LinkedList<>();
    if (json.has("screens")) {
      JSONArray screens = json.getJSONArray("screens");
      for (int i = 0; i < screens.length(); ++i) {
        imageBlock.blockScreens.add(BannerBlockScreen.create(screens.getJSONObject(i)));
      }
    }

    if (json.has("id")) {
      imageBlock.id = json.optString("id");
    }

    imageBlock.imageWidth = 100;
    if (json.has("imageWidth") && json.optInt("imageWidth") > 0) {
      imageBlock.imageWidth = json.optInt("imageWidth");
    }

    imageBlock.imageHeight = 100;
    if (json.has("imageHeight") && json.optInt("imageHeight") > 0) {
      imageBlock.imageHeight = json.optInt("imageHeight");
    }

    return imageBlock;
  }
}
