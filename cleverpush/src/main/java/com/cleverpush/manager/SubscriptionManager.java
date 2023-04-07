package com.cleverpush.manager;

import com.cleverpush.listener.SubscribedCallbackListener;

import org.json.JSONObject;

public interface SubscriptionManager {

  enum SubscriptionManagerType {
    FCM,
    HMS,
    ADM,
  }

  ;

  void subscribe(JSONObject channelConfig, SubscribedCallbackListener callback);

  void checkChangedPushToken(JSONObject channelConfig);

  void checkChangedPushToken(JSONObject channelConfig, String changedToken);

  SubscriptionManagerType getType();
}
