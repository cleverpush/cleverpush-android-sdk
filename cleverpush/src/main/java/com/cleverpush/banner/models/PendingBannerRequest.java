package com.cleverpush.banner.models;

public class PendingBannerRequest {
  public String notificationId;
  public String channelId;

  public PendingBannerRequest(String notificationId, String channelId) {
    this.notificationId = notificationId;
    this.channelId = channelId;
  }
}
