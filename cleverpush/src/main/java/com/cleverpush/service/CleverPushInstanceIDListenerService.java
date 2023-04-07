package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import com.cleverpush.util.Logger;

// FirebaseInstanceIdService was removed in firebase-messaging:18.0.0
@Deprecated
public class CleverPushInstanceIDListenerService {

  public void CleverPushInstanceIDListenerService() {
    Logger.w(LOG_TAG,
        "CleverPushInstanceIDListenerService is deprecated. Please remove it from your AndroidManifest.xml");
  }
}
