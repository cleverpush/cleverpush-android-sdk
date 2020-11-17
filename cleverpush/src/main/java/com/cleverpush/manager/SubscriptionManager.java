package com.cleverpush.manager;

import org.json.JSONObject;

public interface SubscriptionManager {

   interface RegisteredHandler {
      void complete(String id);
   }

   void subscribe(JSONObject channelConfig, RegisteredHandler callback);

   void tokenCallback(String token);

   String getProviderName();
}
