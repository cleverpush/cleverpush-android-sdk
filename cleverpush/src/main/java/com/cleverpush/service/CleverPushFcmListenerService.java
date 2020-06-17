package com.cleverpush.service;

import android.util.Log;

import com.cleverpush.Notification;
import com.cleverpush.Subscription;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

public class CleverPushFcmListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d("CleverPush", "onMessageReceived");

        Map data = message.getData();
        if (data.size() > 0) {

            Log.d("CleverPush", "Notification data: " + data.toString());

            String notificationStr = (String) data.get("notification");
            String subscriptionStr = (String) data.get("subscription");

            Gson gson = new Gson();
            Notification notification = gson.fromJson(notificationStr, Notification.class);
            Subscription subscription = gson.fromJson(subscriptionStr, Subscription.class);

            NotificationDataProcessor.process(this, notificationStr, notification, subscriptionStr, subscription);
        } else {
            Log.e("CleverPush", "Notification data is empty");
        }
    }
}
