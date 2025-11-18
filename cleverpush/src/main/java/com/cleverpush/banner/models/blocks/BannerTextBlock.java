package com.cleverpush.banner.models.blocks;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

public final class BannerTextBlock extends BannerBlock {
  private String text;
  private String color;
  private String darkColor;
  private int size;
  private Alignment alignment;
  private String family = null;
  private String id;
  private List<BannerBlockScreen> blockScreens;
  private String html;

  private BannerTextBlock() {
  }

  public String getText() {
    return text;
  }

  public String getColor() {
    return color;
  }

  public String getDarkColor() {
    return darkColor;
  }

  public int getSize() {
    return size;
  }

  public Alignment getAlignment() {
    return alignment;
  }

  public String getFamily() {
    return family;
  }

  public String getId() {
    return id;
  }

  public List<BannerBlockScreen> getBlocks() {
    return blockScreens;
  }

  public String getHtml() {
    return html;
  }

  public static BannerTextBlock createTextBlock(JSONObject json) throws JSONException {
    BannerTextBlock textBlock = new BannerTextBlock();

    textBlock.type = BannerBlockType.Text;
    textBlock.text = json.optString("text");

    textBlock.color = json.optString("color");
    if (json.has("darkColor") && !json.optString("darkColor").isEmpty()) {
      textBlock.darkColor = json.optString("darkColor");
    }

    textBlock.size = json.getInt("size");
    textBlock.alignment = Alignment.fromString(json.getString("alignment"));
    if (json.has("family") && !json.optString("family").isEmpty()) {
      textBlock.family = json.optString("family");
    }

    textBlock.blockScreens = new LinkedList<>();
    if (json.has("screens")) {
      JSONArray screens = json.getJSONArray("screens");
      for (int i = 0; i < screens.length(); ++i) {
        textBlock.blockScreens.add(BannerBlockScreen.create(screens.getJSONObject(i)));
      }
    }

    if (json.has("id")) {
      textBlock.id = json.optString("id");
    }

    if (json.has("html")) {
      textBlock.html = json.optString("html");
    }

    return textBlock;
  }
}
