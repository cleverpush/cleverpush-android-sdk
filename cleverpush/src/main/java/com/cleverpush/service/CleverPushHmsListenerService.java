package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import com.cleverpush.util.Logger;

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
    Logger.d(LOG_TAG, "CleverPushHmsListenerService onNewToken: " + token);
    SubscriptionManager manager = CleverPush.getInstance(this).getSubscriptionManager();
    if (manager.getType() == SubscriptionManager.SubscriptionManagerType.HMS) {
      ((SubscriptionManagerHMS) manager).tokenCallback(token);
    }
  }

  @Override
  public void onMessageReceived(RemoteMessage message) {
    try {
      String dataStr = message.getData();
      if (dataStr != null) {
        Gson gson = new Gson();
        RemoteMessageData messageData = gson.fromJson(dataStr, RemoteMessageData.class);

        Notification notification = messageData.getNotification();
        Subscription subscription = messageData.getSubscription();

        if (notification != null && subscription != null) {
          String notificationStr = gson.toJson(notification);
          String subscriptionStr = gson.toJson(subscription);

          notification.setRawPayload(notificationStr);
          subscription.setRawPayload(subscriptionStr);
          NotificationDataProcessor.process(this, notification, subscription);
        }
      } else {
        Logger.e(LOG_TAG, "HMS Notification data is null");
      }
    } catch (Exception exception) {
      Logger.e(LOG_TAG, "Error in HMS onMessageReceived handler", exception);
    }
  }
}
