package com.cleverpush.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;

import org.json.JSONException;
import org.json.JSONObject;

abstract class SubscriptionManagerBase implements SubscriptionManager {
    RegisteredHandler registeredHandler;
    Context context;

    SubscriptionManagerBase(Context context) {
        this.context = context;
    }

    @Override
    public void subscribe(RegisteredHandler callback) {
        this.registeredHandler = callback;
    }

    void syncSubscription(String token) {
        syncSubscription(token, null);
    }

    void syncSubscription(String token, String senderId) {
        syncSubscription(token, senderId, false);
    }

    void syncSubscription(String token, String senderId, boolean retry) {
        Log.d("CleverPush", "syncSubscription");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);

        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (channelId == null) {
            Log.d("CleverPush", "channelId in preferences not found");
            return;
        }

        JSONObject jsonBody = new JSONObject();
        try {
            if (this.getProviderName().equals("ADM")) {
                jsonBody.put("admToken", token);
            } else {
                jsonBody.put("fcmToken", token);
                jsonBody.put("fcmId", senderId);
            }
            jsonBody.put("subscriptionId", subscriptionId);
            jsonBody.put("platformName", "Android");
            jsonBody.put("platformVersion", Build.VERSION.RELEASE);
            jsonBody.put("browserType", "SDK");
            jsonBody.put("browserVersion", CleverPush.SDK_VERSION);
        } catch (JSONException e) {
            Log.e("CleverPush", "Error", e);
        }

        CleverPushHttpClient.post("/subscription/sync/" + channelId, jsonBody, new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.has("id")) {
                        String newSubscriptionId = responseJson.getString("id");

                        sharedPreferences.edit().putString(CleverPushPreferences.SUBSCRIPTION_ID, newSubscriptionId).apply();
                        sharedPreferences.edit().putInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, (int) (System.currentTimeMillis() / 1000L)).apply();

                        registeredHandler.complete(newSubscriptionId);
                    }
                } catch (Throwable t) {
                    Log.e("CleverPush", "Error", t);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable t) {
                if (statusCode == 404 || statusCode == 410) {
                    if (!retry) {
                        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_ID).apply();
                        syncSubscription(token, senderId, true);
                        return;
                    }
                }
                Log.e("CleverPush", "Failed while sync subscription request - " + statusCode + " - " + response, t);
            }
        });
    }
}
