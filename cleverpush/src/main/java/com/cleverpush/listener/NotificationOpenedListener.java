package com.cleverpush.listener;

import android.app.Activity;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationOpenedListener extends NotificationOpenedListenerBase {
  void notificationOpened(NotificationOpenedResult result);
}

