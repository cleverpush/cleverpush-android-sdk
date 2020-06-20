package com.cleverpush.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.Subscription;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class NotificationDataProcessor {
    public static void process(Context context, String notificationStr, Notification notification, String subscriptionStr, Subscription subscription) {
        if (notification == null || subscription == null) {
            return;
        }

        String notificationId = notification.getId();
        String subscriptionId = subscription.getId();

        if (notificationId == null || subscriptionId == null) {
            return;
        }

        CleverPush cleverPush = CleverPush.getInstance(context);

        boolean dontShowNotification = false;

        try {
            boolean callbackReceivedListener = cleverPush.isNotificationReceivedListenerCallback();

            NotificationOpenedResult result = new NotificationOpenedResult();
            result.setNotification(notification);
            result.setSubscription(subscription);

            if (callbackReceivedListener) {
                dontShowNotification = !cleverPush.fireNotificationReceivedCallbackListener(result);
            } else {
                if (NotificationService.getInstance().applicationInForeground(context)) {
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
                NotificationService.getInstance().createAndShowCarousel(context, notification, notificationStr, subscriptionStr);
            } else {
                NotificationService.getInstance().sendNotification(context, notification, notificationStr, subscriptionStr);
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
            if (notifications != null) {
                notifications.add(notificationStr);
            }
            editor.remove(CleverPushPreferences.NOTIFICATIONS).apply();
            editor.putStringSet(CleverPushPreferences.NOTIFICATIONS, notifications);
            editor.putString(CleverPushPreferences.LAST_NOTIFICATION_ID, notificationId);
            editor.commit();
        } catch (Exception e) {
            Log.e("CleverPush", "Error saving notification to shared preferences", e);
        }
    }
}
