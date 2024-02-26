package com.cleverpush.banner.models;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BannerAction {
  private String type;
  private String name;
  private String url;
  private String urlType;
  private boolean dismiss;
  private boolean openInWebView;
  private boolean openBySystem;
  private List<String> tags;
  private List<String> topics;
  private String attributeId;
  private String attributeValue;
  private String screen;
  private Map<String, Object> customData;
  private String blockId;
  private String multipleScreenId;

  private BannerAction() {
  }

  public String getType() {
    return type;
  }

  public String getScreen() {
    return screen;
  }

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getUrlType() {
    return urlType;
  }

  public boolean getDismiss() {
    return dismiss;
  }

  public boolean isOpenInWebView() {
    return openInWebView;
  }

  public boolean isOpenBySystem() {
    return openBySystem;
  }

  public List<String> getTags() {
    return tags;
  }

  public List<String> getTopics() {
    return topics;
  }

  public String getAttributeId() {
    return attributeId;
  }

  public String getAttributeValue() {
    return attributeValue;
  }

  public Map<String, Object> getCustomData() {
    return customData;
  }

  public String getBlockId() {
    return blockId;
  }

  public void setBlockId(String blockId) {
    this.blockId = blockId;
  }

  public String getMultipleScreenId() {
    return multipleScreenId;
  }

  public void setMultipleScreenId(String multipleScreenId) {
    this.multipleScreenId = multipleScreenId;
  }

  public static BannerAction create(JSONObject json) throws JSONException {
    BannerAction banner = new BannerAction();

    if (json != null) {
      banner.type = json.optString("type");
      banner.name = json.optString("name");
      banner.url = json.optString("url");
      banner.urlType = json.optString("urlType");
      banner.dismiss = json.optBoolean("dismiss");
      banner.screen = json.optString("screen");

      if (json.has("openInWebview")) {
        banner.openInWebView = json.optBoolean("openInWebview");
      }

      if (json.has("openBySystem")) {
        banner.openBySystem = json.optBoolean("openBySystem");
      }

      JSONArray topicsArray = json.optJSONArray("topics");
      if (topicsArray != null) {
        banner.topics = new ArrayList<>();
        for (int i = 0; i < topicsArray.length(); ++i) {
          String topic = topicsArray.optString(i);
          if (topic != null) {
            banner.topics.add(topic);
          }
        }
      }

      JSONArray tagsArray = json.optJSONArray("tags");
      if (tagsArray != null) {
        banner.tags = new ArrayList<>();
        for (int i = 0; i < tagsArray.length(); ++i) {
          String tag = tagsArray.optString(i);
          if (tag != null) {
            banner.tags.add(tag);
          }
        }
      }

      banner.attributeId = json.optString("attributeId");
      banner.attributeValue = json.optString("attributeValue");
    }

    return banner;
  }

  public static BannerAction create(String type, Map<String, Object> customData) {
    BannerAction banner = new BannerAction();

    banner.customData = customData;
    banner.type = type;

    return banner;
  }
}
