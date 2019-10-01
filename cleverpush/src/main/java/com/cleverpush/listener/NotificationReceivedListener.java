package com.cleverpush.listener;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationReceivedListener {
    void notificationReceived(NotificationOpenedResult result);
}
