package com.cleverpush.service;

import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.RemoteMessageData;
import com.cleverpush.Subscription;
import com.google.gson.Gson;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

public class CleverPushHmsListenerService extends HmsMessageService {
    @Override
    public void onNewToken(String token) {
        Log.d("CleverPush", "CleverPushHmsListenerService onNewToken: " + token);
        CleverPush.getInstance(this).getSubscriptionManager().tokenCallback(token);
    }

    @Override
    public void onMessageReceived(RemoteMessage message) {
        String dataStr = message.getData();

        if (dataStr != null) {
            Gson gson = new Gson();
            RemoteMessageData messageData = gson.fromJson(dataStr, RemoteMessageData.class);

            Notification notification = messageData.getNotification();
            String notificationStr = gson.toJson(notification);

            Subscription subscription = messageData.getSubscription();
            String subscriptionStr = gson.toJson(subscription);

            notification.setRawPayload(notificationStr);
            subscription.setRawPayload(subscriptionStr);

            NotificationDataProcessor.process(this, notification, subscription);
        }
    }
}
