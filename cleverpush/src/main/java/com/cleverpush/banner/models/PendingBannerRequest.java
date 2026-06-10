package com.cleverpush.banner.models;

public class PendingBannerRequest {
  private final String notificationId;
  private final String channelId;

  public PendingBannerRequest(String notificationId, String channelId) {
    this.notificationId = notificationId;
    this.channelId = channelId;
  }

  public String getNotificationId() {
    return notificationId;
  }

  public String getChannelId() {
    return channelId;
  }
}
