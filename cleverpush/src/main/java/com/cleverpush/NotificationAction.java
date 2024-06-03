package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NotificationAction implements Serializable {
  @SerializedName("title")
  String title;
  @SerializedName("url")
  String url;
  @SerializedName("icon")
  String icon;
  @SerializedName("phone")
  String phone;
  @SerializedName("id")
  String id;
  @SerializedName("type")
  String type;

  public String getTitle() {
    return title;
  }

  public String getUrl() {
    return url;
  }

  public String getIcon() {
    return icon;
  }

  public String getPhone() {
    return phone;
  }

  public String getId() {
    return id;
  }

  public String getType() {
    return type;
  }
}
