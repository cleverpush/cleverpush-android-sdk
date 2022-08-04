package com.cleverpush.listener;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationReceivedListenerBase {
    void notificationReceived(NotificationOpenedResult result);
}
