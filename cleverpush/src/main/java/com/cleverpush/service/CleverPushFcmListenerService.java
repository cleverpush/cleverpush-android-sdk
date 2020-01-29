package com.cleverpush.service;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.Subscription;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CleverPushFcmListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d("CleverPush", "onMessageReceived");

        Map data = message.getData();
        if (data.size() > 0) {
            Log.d("CleverPush", "Notification data: " + data.toString());

            Gson gson = new Gson();
            Notification notification = gson.fromJson((String) data.get("notification"), Notification.class);
            Subscription subscription = gson.fromJson((String) data.get("subscription"), Subscription.class);

            if (notification == null || subscription == null) {
                return;
            }

            String notificationId = notification.getId();
            String subscriptionId = subscription.getId();

            if (notificationId == null || subscriptionId == null) {
                return;
            }

            CleverPush cleverPush = CleverPush.getInstance(null);

            boolean dontShowNotification = false;

            try {
                boolean callbackReceivedListener = cleverPush.isNotificationReceivedListenerCallback();

                NotificationOpenedResult result = new NotificationOpenedResult();
                result.setNotification(notification);
                result.setSubscription(subscription);

                if (callbackReceivedListener) {
                    dontShowNotification = !cleverPush.fireNotificationReceivedCallbackListener(result);
                } else {
                    if (NotificationService.getInstance().applicationInForeground(this)) {
                        dontShowNotification = cleverPush.fireNotificationReceivedListener(result);
                    } else {
                        cleverPush.fireNotificationReceivedListener(result);
                    }
                }

            } catch (Exception e) {
                Log.e("CleverPush", "Error checking if application is in foreground", e);
            }

            if (!dontShowNotification) {
                if (notification.getCarouselLength() > 0 && notification.isCarouselEnabled()) {
                    NotificationService.getInstance().createAndShowCarousel(this, notification, data);
                } else {
                    NotificationService.getInstance().sendNotification(this, notification, data);
                }
            }

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("notificationId", notificationId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                Log.e("CleverPush", "Error generating delivered json", e);
            }

            CleverPushHttpClient.post("/notification/delivered", jsonBody, null);

            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Set<String> notifications = sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>());
                notifications.add(gson.toJson(notification));
                editor.remove(CleverPushPreferences.NOTIFICATIONS).apply();
                editor.putStringSet(CleverPushPreferences.NOTIFICATIONS, notifications);
                editor.commit();
            } catch (Exception e) {
                Log.e("CleverPush", "Error saving notification to shared preferences", e);
            }
        } else {
            Log.e("CleverPush", "Notification data is empty");
        }
    }
}
