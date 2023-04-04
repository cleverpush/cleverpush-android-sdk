package com.cleverpush;

import android.app.Activity;

public class NotificationOpenedResult {
  private Notification notification;
  private Subscription subscription;
  private Activity notificationOpenedActivity;

  public Notification getNotification() {
    return notification;
  }

  public void setNotification(Notification notification) {
    this.notification = notification;
  }

  public Subscription getSubscription() {
    return subscription;
  }

  public void setSubscription(Subscription subscription) {
    this.subscription = subscription;
  }

  public Activity getNotificationOpenedActivity() {
    return notificationOpenedActivity;
  }

  public void setNotificationOpenedActivity(Activity notificationOpenedActivity) {
    this.notificationOpenedActivity = notificationOpenedActivity;
  }
}
