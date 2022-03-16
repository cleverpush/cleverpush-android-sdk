package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.RemoteMessageData;
import com.cleverpush.Subscription;
import com.cleverpush.manager.SubscriptionManager;
import com.cleverpush.manager.SubscriptionManagerHMS;
import com.google.gson.Gson;
import com.huawei.hms.push.HmsMessageService;
import com.huawei.hms.push.RemoteMessage;

public class CleverPushHmsListenerService extends HmsMessageService {
    @Override
    public void onNewToken(String token) {
        Log.d(LOG_TAG, "CleverPushHmsListenerService onNewToken: " + token);
        SubscriptionManager manager = CleverPush.getInstance(this).getSubscriptionManager();
        if (manager.getType() == SubscriptionManager.SubscriptionManagerType.HMS) {
            ((SubscriptionManagerHMS) manager).tokenCallback(token);
        }
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
