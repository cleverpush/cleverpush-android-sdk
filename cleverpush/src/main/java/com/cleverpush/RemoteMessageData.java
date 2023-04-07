package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class RemoteMessageData implements Serializable {
  @SerializedName("notification")
  Notification notification;
  @SerializedName("subscription")
  Subscription subscription;

  public Notification getNotification() {
    return notification;
  }

  public Subscription getSubscription() {
    return subscription;
  }
}
