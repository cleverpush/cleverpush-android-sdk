package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.util.Log;

import androidx.annotation.NonNull;

import com.cleverpush.Notification;
import com.cleverpush.Subscription;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import java.util.Map;

public class CleverPushFcmListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(@NonNull RemoteMessage message) {
        Log.d(LOG_TAG, "onMessageReceived");

        try {
			Map<String, String> data = message.getData();
			if (data.size() > 0) {
				Log.d(LOG_TAG, "Notification data: " + data.toString());

				String notificationStr = (String) data.get("notification");
				String subscriptionStr = (String) data.get("subscription");

				if (notificationStr != null && subscriptionStr != null) {
					Gson gson = new Gson();
					Notification notification = gson.fromJson(notificationStr, Notification.class);
					notification.setRawPayload(notificationStr);
					Subscription subscription = gson.fromJson(subscriptionStr, Subscription.class);
					subscription.setRawPayload(subscriptionStr);
					NotificationDataProcessor.process(this, notification, subscription);
				}

			} else {
				Log.e(LOG_TAG, "Notification data is empty");
			}
		} catch (Exception exception) {
			Log.e(LOG_TAG, "Error in FCM onMessageReceived handler", exception);
		}
    }

    @Override
	public void onNewToken(@NonNull String token) {

	}
}
