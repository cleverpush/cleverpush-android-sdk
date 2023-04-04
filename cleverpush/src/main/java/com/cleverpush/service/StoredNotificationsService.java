package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;
import com.cleverpush.util.Logger;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationList;
import com.cleverpush.listener.NotificationFromApiCallbackListener;
import com.cleverpush.listener.NotificationsCallbackListener;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StoredNotificationsService {

  public static Set<Notification> getNotifications(SharedPreferences sharedPreferences) {
    return StoredNotificationsService.getNotificationsFromLocal(sharedPreferences);
  }

  public static void getNotifications(SharedPreferences sharedPreferences,
                                      NotificationsCallbackListener notificationsCallbackListener) {
    Set<Notification> localNotifications = StoredNotificationsService.getNotificationsFromLocal(sharedPreferences);

    notificationsCallbackListener.ready(localNotifications);
  }

  public static StoredNotificationsCursor getCombinedNotifications(String channelId,
                                                                   SharedPreferences sharedPreferences, int limit) {
    return new StoredNotificationsCursor(channelId, sharedPreferences, limit);
  }

  public static Set<Notification> getNotificationsFromLocal(SharedPreferences sharedPreferences) {
    Gson gson = new Gson();

    String notificationsJson = sharedPreferences.getString(CleverPushPreferences.NOTIFICATIONS_JSON, null);
    if (notificationsJson != null) {
      try {
        List<Notification> notifications = gson.fromJson(notificationsJson, NotificationList.class);
        return new HashSet<>(notifications);
      } catch (Exception ex) {
        Logger.e(LOG_TAG, "error while getting stored notifications", ex);
      }
    }

    // deprecated
    Set<String> encodedNotifications =
        sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>());
    Set<Notification> notifications = new HashSet<>();
    if (encodedNotifications != null) {
      for (String encodedNotification : encodedNotifications) {
        Notification notification = gson.fromJson(encodedNotification, Notification.class);
        notifications.add(notification);
      }
    }

    return notifications;
  }

  public static void getReceivedNotificationsFromApi(String channelId, SharedPreferences sharedPreferences, int limit,
                                                     int skip,
                                                     NotificationFromApiCallbackListener notificationFromApiCallbackListener) {
    StringBuilder url =
        new StringBuilder("/channel/" + channelId + "/received-notifications?limit=" + limit + "&skip=" + skip);
    ArrayList<String> subscriptionTopics =
        new ArrayList<String>(StoredNotificationsService.getSubscriptionTopics(sharedPreferences));

    Logger.d(LOG_TAG, "getReceivedNotificationsFromApi: " + url);

    for (int i = 0; i < subscriptionTopics.size(); i++) {
      url.append("&topics[]=").append(subscriptionTopics.get(i));
    }

    CleverPushHttpClient.get(url.toString(), new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        if (response != null) {
          Gson gson = new Gson();
          try {
            JSONObject notificationObject = new JSONObject(response);
            List<Notification> notifications = gson.fromJson(
                notificationObject.getJSONArray("notifications").toString(),
                NotificationList.class
            );
            notificationFromApiCallbackListener.ready(notifications);
          } catch (Exception ex) {
            Logger.e(LOG_TAG, "error while getting stored notifications", ex);
          }
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        Logger.e(LOG_TAG, "Error got Response - HTTP " + statusCode);
      }
    });
  }

  public static Set<String> getSubscriptionTopics(SharedPreferences sharedPreferences) {
    return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>());
  }
}
