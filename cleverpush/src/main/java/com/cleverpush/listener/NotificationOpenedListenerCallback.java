package com.cleverpush.listener;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationOpenedListenerCallback {
    void notificationOpened(NotificationOpenedResult result, AppActivity finishActivity);
}