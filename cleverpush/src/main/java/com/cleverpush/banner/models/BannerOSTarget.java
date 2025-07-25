package com.cleverpush.banner.models;

import org.json.JSONException;
import org.json.JSONObject;

public class BannerOSTarget {
  private String platform;
  private String operator;
  private String target;
  private String from;
  private String to;

  public String getPlatform() {
    return platform;
  }

  public String getOperator() {
    return operator;
  }

  public String getTarget() {
    return target;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }

  public static BannerOSTarget create(JSONObject json) throws JSONException {
    BannerOSTarget bannerOSTarget = new BannerOSTarget();

    if (json.has("platform")) {
      bannerOSTarget.platform = json.getString("platform");
    }
    if (json.has("operator")) {
      bannerOSTarget.operator = json.getString("operator");
    }
    if (json.has("target")) {
      bannerOSTarget.target = json.getString("target");
    }
    if (json.has("from")) {
      bannerOSTarget.from = json.getString("from");
    }
    if (json.has("to")) {
      bannerOSTarget.to = json.getString("to");
    }

    return bannerOSTarget;
  }
}
