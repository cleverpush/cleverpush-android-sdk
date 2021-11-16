package com.cleverpush.manager;

import com.cleverpush.listener.SubscribedListener;

import org.json.JSONObject;

public interface SubscriptionManager {

    enum SubscriptionManagerType {
        FCM,
        HMS,
        ADM,
    };

    void subscribe(JSONObject channelConfig, SubscribedListener callback);

    void checkChangedPushToken(JSONObject channelConfig);

    SubscriptionManagerType getType();
}
