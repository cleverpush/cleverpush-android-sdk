package com.cleverpush;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

public class NotificationOpenedProcessor {

    public static void processIntent(Context context, Intent intent) {
        Gson gson = new Gson();
        Notification notification = gson.fromJson(intent.getStringExtra("notification"), Notification.class);
        Subscription subscription = gson.fromJson(intent.getStringExtra("subscription"), Subscription.class);

        if (notification == null || subscription == null) {
            return;
        }

        String notificationId = notification.getId();
        String subscriptionId = subscription.getId();

        if (notificationId == null || subscriptionId == null) {
            return;
        }

        NotificationOpenedResult result = new NotificationOpenedResult();
        result.setNotification(notification);
        result.setSubscription(subscription);

        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("notificationId", notificationId);
            jsonBody.put("subscriptionId", subscriptionId);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CleverPushHttpClient.post("/notification/clicked", jsonBody, null);

        CleverPush.getInstance(context).fireNotificationOpenedListener(result);
    }
}
