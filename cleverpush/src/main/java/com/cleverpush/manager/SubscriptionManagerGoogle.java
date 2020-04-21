package com.cleverpush.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.FcmSenderIdListener;

import org.json.JSONObject;

import java.io.IOException;

abstract class SubscriptionManagerGoogle extends SubscriptionManagerBase {
    private static int REGISTRATION_RETRY_COUNT = 5;
    private static int REGISTRATION_RETRY_BACKOFF_MS = 10_000;

    public SubscriptionManagerGoogle(Context context) {
        super(context);
    }

    public abstract String getProviderName();

    abstract String getToken(String senderId) throws Throwable;

    @Override
    public void subscribe(RegisteredHandler callback) {
        super.subscribe(callback);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);

        getFcmSenderId(channelId, fcmSenderId -> {
            if (isValidProjectNumber(fcmSenderId, callback)) {
                internalSubscribe(fcmSenderId);
            }
        });
    }

    protected static void getFcmSenderId(String channelId, FcmSenderIdListener listener) {
        CleverPushHttpClient.get("/channel/" + channelId + "/fcm-params", new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.has("fcmSenderId")) {
                        listener.complete(responseJson.getString("fcmSenderId"));
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e("CleverPush", "Getting FCM Sender ID failed");
            }
        });
    }

    private void internalSubscribe(String senderId) {
        try {
            registerInBackground(senderId);
        } catch (Throwable t) {
            Log.e("CleverPush",
                    "Could not register with "
                            + getProviderName() +
                            " due to an issue with your AndroidManifest.xml or with 'Google Play services'.",
                    t
            );
        }
    }

    private Thread registerThread;

    private synchronized void registerInBackground(final String senderId) {
        // If any thread is still running, don't create a new one
        if (registerThread != null && registerThread.isAlive())
            return;

        registerThread = new Thread(new Runnable() {
            public void run() {
                for (int currentRetry = 0; currentRetry < REGISTRATION_RETRY_COUNT; currentRetry++) {
                    boolean finished = attemptRegistration(senderId, currentRetry);
                    if (finished) {
                        return;
                    }
                    try {
                        Thread.sleep(REGISTRATION_RETRY_BACKOFF_MS * (currentRetry + 1));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        registerThread.start();
    }

    private boolean firedCallback;

    private boolean attemptRegistration(String senderId, int currentRetry) {
        try {
            String token = getToken(senderId);
            Log.i("CleverPush", "Device registered, push token: " + token);

            if (token.equals("BLACKLISTED")) {
                return false;
            }

            this.syncSubscription(token, senderId);

            return true;
        } catch (IOException e) {
            if (!"SERVICE_NOT_AVAILABLE".equals(e.getMessage())) {
                Log.e("CleverPush", "Error Getting " + getProviderName() + " Token ", e);
                if (!firedCallback)
                    registeredHandler.complete(null);
                return true;
            } else {
                if (currentRetry >= (REGISTRATION_RETRY_COUNT - 1))
                    Log.e("CleverPush", "Retry count of " + REGISTRATION_RETRY_COUNT + " exceed! Could not get a " + getProviderName() + " Token.", e);
                else {
                    Log.i("CleverPush", "Google Play services returned SERVICE_NOT_AVAILABLE error. Current retry count: " + currentRetry, e);
                    if (currentRetry == 2) {
                        // Retry 3 times before firing a null response and continuing a few more times.
                        registeredHandler.complete(null);
                        firedCallback = true;
                        return true;
                    }
                }
            }
        } catch (Throwable t) {
            Log.e("CleverPush", "Unknown error getting " + getProviderName() + " Token", t);
            registeredHandler.complete(null);
            return true;
        }

        return false;
    }

    private boolean isValidProjectNumber(String senderId, SubscriptionManager.RegisteredHandler callback) {
        boolean isProjectNumberValidFormat;
        try {
            Float.parseFloat(senderId);
            isProjectNumberValidFormat = true;
        } catch (Throwable t) {
            isProjectNumberValidFormat = false;
        }

        if (!isProjectNumberValidFormat) {
            Log.e("CleverPush", "Missing FCM Sender ID");
            callback.complete(null);
            return false;
        }
        return true;
    }
}
