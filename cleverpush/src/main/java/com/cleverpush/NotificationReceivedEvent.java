package com.cleverpush;

import android.content.Context;

public class NotificationReceivedEvent {

  Context context = null;
  Notification notification = null;
  boolean preventDefaultCalled = false;

  public NotificationReceivedEvent(Context context, Notification notification) {
    this.context = context;
    this.notification = notification;
  }

  public Context getContext() {
    return this.context;
  }

  public Notification getNotification() {
    return this.notification;
  }

  public void preventDefault() {
    this.preventDefaultCalled = true;
  }

  public boolean isPreventDefault() {
    return this.preventDefaultCalled;
  }
}