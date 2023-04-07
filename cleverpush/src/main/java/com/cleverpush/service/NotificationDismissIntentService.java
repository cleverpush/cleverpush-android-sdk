package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.IntentService;
import android.content.Intent;

import com.cleverpush.Notification;
import com.cleverpush.util.Logger;

import com.cleverpush.BadgeHelper;
import com.cleverpush.CleverPush;

public class NotificationDismissIntentService extends IntentService {
  public NotificationDismissIntentService(String name) {
    super(name);
  }

  public NotificationDismissIntentService() {
    super("NotificationIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Logger.d(LOG_TAG, "NotificationDismissIntentService: onHandleIntent");

    boolean badgeEnabled = true;
    try {
      Notification notification = (Notification) intent.getSerializableExtra("notification");
      if (notification.getCategory() != null && notification.getCategory().getBadgeDisabled()) {
        badgeEnabled = false;
      }
    } catch (Exception ignore) {
    }

    if (badgeEnabled) {
      CleverPush cleverPush = CleverPush.getInstance(this);
      BadgeHelper.update(this, cleverPush.getIncrementBadge());
    }
  }
}
