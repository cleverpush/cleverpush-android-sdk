package com.cleverpush.manager;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.SubscribedListener;
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
            Log.d(LOG_TAG, "FirebaseMessaging.getToken not found, attempting to use FirebaseInstanceId.getToken");
        }
        return getTokenWithClassFirebaseInstanceId(senderId);
    }

    private void initFirebaseApp(String senderId) {
        if (firebaseApp != null) {
            return;
        }

        firebaseApp = FirebaseApp.getInstance();
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

        throw new Error("Reflection error in FirebaseInstanceId.getInstance(firebaseApp).getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE)", exception);
    }

    /**
     * Will use to get Firebase token if firebase-message newer than 20.0.0
     */
    @SuppressWarnings("unchecked")
    @WorkerThread
    private String getTokenWithClassFirebaseMessaging() throws Exception {
        Exception exception;
        try {
            Class<?> FirebaseInstanceIdClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            Method getInstanceMethod = FirebaseInstanceIdClass.getMethod("getInstance");
            Object instanceId = getInstanceMethod.invoke(null, null);
            Method getTokenMethod = FirebaseInstanceIdClass.getMethod("getToken");
            Task<String> tokenTask = (Task<String>) getTokenMethod.invoke(instanceId, null);
            return Tasks.await(tokenTask);
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = new IllegalAccessException();
        } catch (InvocationTargetException e) {
            exception = e;
        }
        throw exception;
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
    public void subscribe(JSONObject channelConfig, SubscribedListener subscribedListener) {
        String senderId = this.getSenderIdFromConfig(channelConfig);
        if (senderId == null) {
            Log.e(LOG_TAG, "SubscriptionManager: Getting FCM Sender ID failed");
            subscribedListener.subscribed(null);
            return;
        }

        try {
            subscribeInBackground(senderId, subscribedListener);
        } catch (Throwable t) {
            Log.e(LOG_TAG,
                    "Could not register with FCM due to an issue with your AndroidManifest.xml or with FCM.",
                    t
            );
        }
    }

    @Override
    public void checkChangedPushToken(JSONObject channelConfig) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        String existingToken = sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null);

        if (existingToken == null) {
            return;
        }

        new Thread(() -> {
            String senderId = this.getSenderIdFromConfig(channelConfig);
            if (senderId == null) {
                Log.e(LOG_TAG, "SubscriptionManager: Getting FCM Sender ID failed");
                return;
            }

            try {
                String newToken = getTokenAttempt(senderId);
                if (newToken != null && !newToken.equals(existingToken)) {
                    this.syncSubscription(newToken, subscriptionId -> Log.i(LOG_TAG, "Synchronized new FCM token: " + newToken));
                } else {
                    Log.d(LOG_TAG, "FCM token has not changed: " + newToken);
                }
            } catch (Throwable throwable) {
                Log.e(LOG_TAG, "Unknown error getting FCM Token", throwable);
            }
        }, THREAD_NAME).start();
    }

    private synchronized void subscribeInBackground(final String senderId, SubscribedListener subscribedListener) {
        if (registerThread != null && registerThread.isAlive()) {
            return;
        }

        registerThread = new Thread(() -> {
            try {
                for (int currentRetry = 0; currentRetry < REGISTRATION_RETRY_COUNT; currentRetry++) {
                    String token = getTokenAttempt(senderId);
                    if (token != null) {
                        Log.i(LOG_TAG, "Device registered (FCM), push token: " + token);
                        this.syncSubscription(token, subscribedListener, senderId);
                        return;
                    }

                    if (currentRetry >= (REGISTRATION_RETRY_COUNT - 1)) {
                        Log.e(LOG_TAG, "Retry count of " + REGISTRATION_RETRY_COUNT + " exceed! Could not get a FCM Token.");
                    } else {
                        Log.i(LOG_TAG, "FCM returned SERVICE_NOT_AVAILABLE error. Current retry count: " + currentRetry);
                    }

                    try {
                        Thread.sleep(REGISTRATION_RETRY_BACKOFF_MS * (currentRetry + 1));
                    } catch (InterruptedException e) {
                        Log.e(LOG_TAG, "Caught InterruptedException", e);
                    }
                }
            } catch (Throwable throwable) {
                Log.e(LOG_TAG, "Unknown error getting FCM Token", throwable);
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
                Log.e(LOG_TAG, "Error Getting FCM Token ", exception);
                throw exception;
            }

            return null;
        }
    }

    private boolean isValidSenderId(String senderId) {
        try {
            Float.parseFloat(senderId);
            return true;
        } catch (Throwable t) {
            Log.e(LOG_TAG, "Missing FCM Sender ID");
            return false;
        }
    }
}
