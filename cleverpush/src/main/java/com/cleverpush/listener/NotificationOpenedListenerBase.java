package com.cleverpush.listener;

import android.app.Activity;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationOpenedListenerBase {
    void notificationOpened(NotificationOpenedResult result);
}
