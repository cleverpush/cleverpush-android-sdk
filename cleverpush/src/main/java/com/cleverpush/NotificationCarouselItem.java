package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class NotificationCarouselItem implements Serializable {
  @SerializedName("mediaUrl")
  String mediaUrl;

  public String getMediaUrl() {
    return mediaUrl;
  }
}
