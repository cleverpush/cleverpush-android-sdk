package com.cleverpush.manager;

import android.content.Context;

public interface SubscriptionManager {

   interface RegisteredHandler {
      void complete(String id);
   }

   void subscribe(RegisteredHandler callback);

   void tokenCallback(String token);

   String getProviderName();
}
