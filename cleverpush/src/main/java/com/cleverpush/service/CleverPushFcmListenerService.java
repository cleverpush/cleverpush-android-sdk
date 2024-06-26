package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.Subscription;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.Map;

public class CleverPushFcmListenerService extends FirebaseMessagingService {
  @Override
  public void onMessageReceived(@NonNull RemoteMessage message) {
    Logger.d(LOG_TAG, "FCM: onMessageReceived");

    try {
      Map<String, String> data = message.getData();
      if (data.size() > 0) {
        Logger.d(LOG_TAG, "Notification data: " + data.toString());

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
        Logger.e(LOG_TAG, "Notification data is empty");
      }
    } catch (Exception exception) {
      Logger.e(LOG_TAG, "Error in FCM onMessageReceived handler", exception);
    }
  }

  @Override
  public void onNewToken(@NonNull String token) {
    Logger.d(LOG_TAG, "FCM: onNewToken");

    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(this);
    String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);

    if (subscriptionId == null) {
      Logger.d(LOG_TAG, "CleverPushFcmListenerService onNewToken: There is no subscription for CleverPush SDK.");
      return;
    }

    CleverPush cleverPush = CleverPush.getInstance(this);
    cleverPush.getChannelConfig(
        (JSONObject channelConfig) -> cleverPush.getSubscriptionManager().checkChangedPushToken(channelConfig, token));
  }
}
