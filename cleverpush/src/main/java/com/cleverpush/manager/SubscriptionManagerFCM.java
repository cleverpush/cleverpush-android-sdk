package com.cleverpush.manager;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.WorkerThread;

import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.SubscribedCallbackListener;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.FirebaseApp;

import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SubscriptionManagerFCM extends SubscriptionManagerBase {

  private static final int REGISTRATION_RETRY_COUNT = 3;
  private static final int REGISTRATION_RETRY_BACKOFF_MS = 10_000;

  private static final String TOKEN_BLACKLISTED = "BLACKLISTED";
  private static final String ERROR_SERVICE_NOT_AVAILABLE = "SERVICE_NOT_AVAILABLE";
  private static final String THREAD_NAME = "FCM_GET_TOKEN";
  private static final String REGEN_THREAD_NAME = "FCM_REGEN_TOKEN";
  private volatile boolean isRegeneratingPushToken = false;

  private FirebaseApp firebaseApp;
  private Thread registerThread;

  public SubscriptionManagerFCM(Context context) {
    super(context, SubscriptionManagerType.FCM);
  }

  private String getToken(String senderId) throws Throwable {
    initFirebaseApp(senderId);

    try {
      return getTokenWithClassFirebaseMessaging();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "FirebaseMessaging.getToken not found, attempting to use FirebaseInstanceId.getToken", e);
    }

    if (isFirebaseInstanceIdAvailable()) {
      try {
        return getTokenWithClassFirebaseInstanceId(senderId);
      } catch (Throwable t) {
        Logger.e(LOG_TAG, "FirebaseInstanceId fallback failed", t);
      }
    } else {
      Logger.w(LOG_TAG, "FirebaseInstanceId not available on this Firebase version");
    }

    return null;
  }

  private void initFirebaseApp(String senderId) {
    if (firebaseApp != null) {
      return;
    }

    firebaseApp = FirebaseApp.getInstance();
  }

  private boolean isFirebaseInstanceIdAvailable() {
    try {
      Class.forName("com.google.firebase.iid.FirebaseInstanceId");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * Will use to get Firebase token if firebase-message older than 21.0.0
   */
  @Deprecated
  @WorkerThread
  private String getTokenWithClassFirebaseInstanceId(String senderId) throws IOException {
    Exception exception;
    try {
      Class<?> FirebaseInstanceIdClass = Class.forName("com.google.firebase.iid.FirebaseInstanceId");
      Method getInstanceMethod = FirebaseInstanceIdClass.getMethod("getInstance", FirebaseApp.class);
      Object instanceId = getInstanceMethod.invoke(null, firebaseApp);
      Method getTokenMethod = instanceId.getClass().getMethod("getToken", String.class, String.class);
      Object token = getTokenMethod.invoke(instanceId, senderId, "FCM");
      return (String) token;
    } catch (ClassNotFoundException e) {
      exception = e;
    } catch (NoSuchMethodException e) {
      exception = e;
    } catch (IllegalAccessException e) {
      exception = e;
    } catch (InvocationTargetException e) {
      exception = e;
    }

    throw new Error(
        "Reflection error in FirebaseInstanceId.getInstance(firebaseApp).getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE)",
        exception);
  }

  /**
   * Will use to get Firebase token if firebase-message newer than 20.0.0
   */
  @SuppressWarnings("unchecked")
  @WorkerThread
  private String getTokenWithClassFirebaseMessaging() throws Exception {
    try {
      Class<?> firebaseMessagingClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging");

      // getInstance()
      Method getInstanceMethod = firebaseMessagingClass.getMethod("getInstance");
      Object instance = getInstanceMethod.invoke(null);

      // getToken()
      Method getTokenMethod = firebaseMessagingClass.getMethod("getToken");
      Task<String> tokenTask = (Task<String>) getTokenMethod.invoke(instance);

      return Tasks.await(tokenTask);
    } catch (ClassNotFoundException |
             NoSuchMethodException |
             IllegalAccessException |
             InvocationTargetException e) {
      throw e;
    }
  }

  private String getSenderIdFromConfig(JSONObject channelConfig) {
    if (channelConfig == null) {
      return null;
    }

    String senderId = channelConfig.optString("fcmId");
    if (senderId == null || senderId.isEmpty() || !isValidSenderId(senderId)) {
      return null;
    }

    return senderId;
  }

  @Override
  public void subscribe(JSONObject channelConfig, SubscribedCallbackListener subscribedListener) {
    this.lastChannelConfig = channelConfig;
    String senderId = this.getSenderIdFromConfig(channelConfig);
    if (senderId == null) {
      Logger.e(LOG_TAG, "SubscriptionManager: Getting FCM Sender ID failed");
      subscribedListener.onFailure(new Exception("SubscriptionManager: Getting FCM Sender ID failed"));
      return;
    }

    try {
      subscribeInBackground(senderId, subscribedListener);
    } catch (Throwable throwable) {
      Logger.e(LOG_TAG,
          "Could not register with FCM due to an issue with your AndroidManifest.xml or with FCM.",
          throwable
      );
    }
  }

  @Override
  public void checkChangedPushToken(JSONObject channelConfig, String changedToken) {
    this.lastChannelConfig = channelConfig;
    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(this.context);

    // IMPORTANT:
    // Compare against LAST SUCCESSFULLY SYNCED token,
    // not the latest locally cached Firebase token.
    String existingToken = sharedPreferences.getString(CleverPushPreferences.SYNCED_FCM_TOKEN, null);

    if (existingToken == null) {
      Logger.d(LOG_TAG, "No synced FCM token found. Syncing current token.");
    }

    new Thread(() -> {
      String senderId = this.getSenderIdFromConfig(channelConfig);

      if (senderId == null) {
        Logger.e(LOG_TAG, "SubscriptionManager: Getting FCM Sender ID failed");
        return;
      }

      try {
        if (isRegeneratingPushToken) {
          Logger.d(LOG_TAG, "Ignoring checkChangedPushToken because token regeneration is already in progress.");
          return;
        }

        String newToken = changedToken != null ? changedToken : getTokenAttempt(senderId);

        if (newToken == null) {
          Logger.d(LOG_TAG, "checkChangedPushToken: no FCM token available yet, will retry on next sync.");
          return;
        }

        boolean forcedTokenRefresh = changedToken != null;
        boolean tokenMissingLocally = existingToken == null;
        boolean tokenChanged = !newToken.equals(existingToken);

        if (forcedTokenRefresh || tokenMissingLocally || tokenChanged) {

          this.syncSubscription(
                  newToken,
                  new SubscribedCallbackListener() {

                    @Override
                    public void onSuccess(String subscriptionId) {
                      Logger.i(LOG_TAG, "Synchronized FCM token: " + newToken);

                      sharedPreferences.edit()
                              // latest local token
                              .putString(CleverPushPreferences.FCM_TOKEN, newToken)
                              // last successfully synced token
                              .putString(CleverPushPreferences.SYNCED_FCM_TOKEN, newToken)
                              .apply();
                    }

                    @Override
                    public void onFailure(Throwable exception) {
                      Logger.e(LOG_TAG, "Failed to sync FCM token", exception);
                    }
                  },
                  senderId
          );

        } else {
          Logger.d(LOG_TAG, "FCM token has not changed: " + newToken);
        }
      } catch (Throwable throwable) {
        Logger.e(LOG_TAG, "Unknown error getting FCM Token in checkChangedPushToken.", throwable);
      }
    }, THREAD_NAME).start();
  }

  private synchronized void subscribeInBackground(final String senderId,
                                                  SubscribedCallbackListener subscribedListener) {
    if (registerThread != null && registerThread.isAlive()) {
      return;
    }

    registerThread = new Thread(() -> {
      try {
        for (int currentRetry = 0; currentRetry < REGISTRATION_RETRY_COUNT; currentRetry++) {
          String token = getTokenAttempt(senderId);
          if (token != null) {
            Logger.i(LOG_TAG, "Device registered (FCM), push token: " + token);
            this.syncSubscription(token, subscribedListener, senderId);
            return;
          }

          if (currentRetry >= (REGISTRATION_RETRY_COUNT - 1)) {
            Logger.e(LOG_TAG, "Retry count of " + REGISTRATION_RETRY_COUNT + " exceed! Could not get a FCM Token.");
          } else {
            Logger.i(LOG_TAG, "FCM returned SERVICE_NOT_AVAILABLE error. Current retry count: " + currentRetry);
          }

          try {
            Thread.sleep(REGISTRATION_RETRY_BACKOFF_MS * (currentRetry + 1));
          } catch (InterruptedException e) {
            Logger.e(LOG_TAG, "Caught InterruptedException in subscribeInBackground.", e);
          }
        }
      } catch (Throwable throwable) {
        Logger.e(LOG_TAG, "Unknown error getting FCM Token in subscribeInBackground.", throwable);
        subscribedListener.onFailure(throwable);
      }
    }, THREAD_NAME);
    registerThread.start();
  }

  private String getTokenAttempt(String senderId) throws Throwable {
    try {
      String token = getToken(senderId);

      if (token == null || token.equals(TOKEN_BLACKLISTED)) {
        throw new Error("Got invalid token from FCM");
      }

      return token;
    } catch (IOException exception) {
      if (!ERROR_SERVICE_NOT_AVAILABLE.equals(exception.getMessage())) {
        Logger.e(LOG_TAG, "Error Getting FCM Token in getTokenAttempt", exception);
        throw exception;
      }

      return null;
    }
  }

  /**
   * Regenerates the FCM push token by deleting the existing token,
   * requesting a new one from Firebase, and synchronizing it with
   * the CleverPush backend.
   */
  @Override
  protected void regeneratePushToken() {
    final JSONObject channelConfig = this.lastChannelConfig;
    final String senderId = this.getSenderIdFromConfig(channelConfig);
    if (senderId == null) {
      Logger.e(LOG_TAG, "regeneratePushToken: FCM Sender ID is not available, skipping regeneration.");
      return;
    }

    isRegeneratingPushToken = true;

    new Thread(() -> {
      try {
        initFirebaseApp(senderId);

        // 1. Delete the current FCM token so the next getToken() returns a new one.
        try {
          deleteTokenWithClassFirebaseMessaging();
          Logger.d(LOG_TAG, "regeneratePushToken: deleted existing FCM token.");
        } catch (Throwable t) {
          Logger.e(LOG_TAG, "regeneratePushToken: failed to delete existing FCM token, attempting to fetch new token anyway.", t);
        }

        // 2. Clear locally cached tokens so checkChangedPushToken-like flows
        //    do not short-circuit and so the next sync actually pushes the new token.
        SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(this.context);
        sharedPreferences.edit()
            .remove(CleverPushPreferences.FCM_TOKEN)
            .remove(CleverPushPreferences.SYNCED_FCM_TOKEN)
            .apply();

        // 3. Fetch a fresh token from Firebase.
        String newToken = getTokenAttempt(senderId);
        if (newToken == null) {
          Logger.d(LOG_TAG, "regeneratePushToken: no FCM token available yet after regeneration, will retry on next sync.");
          isRegeneratingPushToken = false;
          return;
        }

        Logger.i(LOG_TAG, "regeneratePushToken: obtained new FCM token: " + newToken);

        // 4. Sync the new token with the CleverPush backend.
        this.syncSubscription(newToken, new SubscribedCallbackListener() {
          @Override
          public void onSuccess(String subscriptionId) {
            Logger.i(LOG_TAG, "regeneratePushToken: synchronized regenerated FCM token.");
            sharedPreferences.edit()
                .putString(CleverPushPreferences.FCM_TOKEN, newToken)
                .putString(CleverPushPreferences.SYNCED_FCM_TOKEN, newToken)
                .apply();
            isRegeneratingPushToken = false;
          }

          @Override
          public void onFailure(Throwable exception) {
            Logger.e(LOG_TAG, "regeneratePushToken: failed to sync regenerated FCM token.", exception);
            isRegeneratingPushToken = false;
          }
        }, senderId);
      } catch (Throwable throwable) {
        Logger.e(LOG_TAG, "Unknown error in regeneratePushToken.", throwable);
        isRegeneratingPushToken = false;
      }
    }, REGEN_THREAD_NAME).start();
  }

  /**
   * Reflective call to {@code FirebaseMessaging.getInstance().deleteToken()} so the
   * SDK keeps working even when only the Firebase classes are present at runtime.
   */
  @WorkerThread
  @SuppressWarnings("unchecked")
  private void deleteTokenWithClassFirebaseMessaging() throws Exception {
    Class<?> firebaseMessagingClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging");
    Method getInstanceMethod = firebaseMessagingClass.getMethod("getInstance");
    Object instance = getInstanceMethod.invoke(null);

    Method deleteTokenMethod = firebaseMessagingClass.getMethod("deleteToken");
    Task<Void> deleteTask = (Task<Void>) deleteTokenMethod.invoke(instance);
    if (deleteTask != null) {
      Tasks.await(deleteTask);
    }
  }

  private boolean isValidSenderId(String senderId) {
    try {
      Float.parseFloat(senderId);
      return true;
    } catch (Throwable throwable) {
      Logger.e(LOG_TAG, "Missing FCM Sender ID");
      return false;
    }
  }
}
