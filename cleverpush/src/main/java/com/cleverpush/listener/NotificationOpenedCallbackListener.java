package com.cleverpush.listener;

import android.app.Activity;

import com.cleverpush.NotificationOpenedResult;

public abstract class NotificationOpenedCallbackListener implements NotificationOpenedListenerBase {
    public void notificationOpened(NotificationOpenedResult result) {

    }

    /**
     * @param result notification open result
     */
    public abstract void notificationOpenedCallback(NotificationOpenedResult result, Activity notificationOpenedActivity);
}
