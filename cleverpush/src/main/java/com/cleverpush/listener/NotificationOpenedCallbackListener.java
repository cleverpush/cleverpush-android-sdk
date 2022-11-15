package com.cleverpush.listener;

import com.cleverpush.NotificationOpenedResult;

public interface NotificationOpenedCallbackListener {
    void notificationOpenedCallback(NotificationOpenedResult result, FinishActivity finishActivity);
}
