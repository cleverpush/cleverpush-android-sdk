package com.cleverpush.service;

import android.util.Log;

import com.cleverpush.Notification;
import com.cleverpush.RemoteMessageData;
import com.cleverpush.Subscription;
import com.cleverpush.manager.SubscriptionManagerHMS;
import com.google.gson.Gson;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

public class CleverPushHmsListenerService extends HmsMessageService {
    @Override
    public void onNewToken(String token) {
        Log.d("CleverPush", "CleverPushHmsListenerService onNewToken: " + token);
        SubscriptionManagerHMS.fireCallback(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String dataStr = message.getData();

        Gson gson = new Gson();
        RemoteMessageData messageData = gson.fromJson(dataStr, RemoteMessageData.class);

        Notification notification = messageData.getNotification();
        Subscription subscription = messageData.getSubscription();

        String notificationStr = gson.toJson(notification);
        String subscriptionStr = gson.toJson(subscription);

        NotificationDataProcessor.process(this, notificationStr, notification, subscriptionStr, subscription);
    }
}
