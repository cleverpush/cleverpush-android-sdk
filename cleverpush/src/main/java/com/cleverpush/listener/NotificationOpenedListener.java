package com.cleverpush.listener;

import android.app.Activity;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationOpenedListener {
    void notificationOpened(NotificationOpenedResult result, Activity finishActivity);
}

