package com.cleverpush.listener;

import com.cleverpush.NotificationOpenedResult;

public abstract class NotificationReceivedCallbackListener implements NotificationReceivedListenerBase {
    public void notificationReceived(NotificationOpenedResult result) {

    }

    /**
     *
     * @param result notification open result
     * @return true if the notification should be shown
     */
    public abstract boolean notificationReceivedCallback(NotificationOpenedResult result);
}
