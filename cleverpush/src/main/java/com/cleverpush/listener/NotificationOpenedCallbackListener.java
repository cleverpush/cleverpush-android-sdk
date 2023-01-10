package com.cleverpush.listener;

import android.app.Activity;

import com.cleverpush.NotificationOpenedResult;

public abstract class NotificationOpenedCallbackListener implements NotificationOpenedListenerBase {
    public void notificationOpened(NotificationOpenedResult result) {

    }

    /**
     *
     * @param result notification open result
     * @return true if the notification should be shown
     */
    public abstract boolean notificationOpenedCallback(NotificationOpenedResult result, Activity finishActivity);
}
