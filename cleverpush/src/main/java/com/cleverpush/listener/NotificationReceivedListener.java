package com.cleverpush.listener;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationReceivedListener extends NotificationReceivedListenerBase {
  void notificationReceived(NotificationOpenedResult result);
}
