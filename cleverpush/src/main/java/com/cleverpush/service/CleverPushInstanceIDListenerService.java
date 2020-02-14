package com.cleverpush.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.FcmSenderIdListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class CleverPushInstanceIDListenerService extends FirebaseInstanceIdService {

    private static final String TAG = "CPInstanceIDLS";

    @Override
    public void onTokenRefresh() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (channelId == null) {
            return;
        }

        getFcmSenderId(channelId, fcmSenderId -> {
            try {
                String token = FirebaseInstanceId.getInstance().getToken("FCM", fcmSenderId);

                sharedPreferences.edit().putString(CleverPushPreferences.FCM_TOKEN, token).apply();

                sendRegistrationToServer(sharedPreferences, channelId, subscriptionId, token);

                sharedPreferences.edit().putBoolean(CleverPushPreferences.SENT_TOKEN_TO_SERVER, true).apply();
            } catch (Exception e) {
                Log.d(TAG, "Failed to complete token refresh", e);
                sharedPreferences.edit().putBoolean(CleverPushPreferences.SENT_TOKEN_TO_SERVER, false).apply();
            }
            Intent registrationComplete = new Intent(CleverPushPreferences.REGISTRATION_COMPLETE);
            LocalBroadcastManager.getInstance(this).sendBroadcast(registrationComplete);
        });
    }

    private static void getFcmSenderId(String channelId, FcmSenderIdListener listener) {
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

            }
        });
    }

    private void sendRegistrationToServer(SharedPreferences sharedPreferences, String channelId, String subscriptionId, String token) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("fcmToken", token);
            jsonBody.put("subscriptionId", subscriptionId);
            jsonBody.put("platformName", "Android");
            jsonBody.put("platformVersion", Build.VERSION.RELEASE);
            jsonBody.put("browserType", "SDK");
            jsonBody.put("browserVersion", CleverPush.SDK_VERSION);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        CleverPushHttpClient.post("/subscription/sync/" + channelId, jsonBody, new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.has("id")) {
                        sharedPreferences.edit().putString(CleverPushPreferences.SUBSCRIPTION_ID, responseJson.getString("id")).apply();
                    }
                    if (responseJson.has("topics")) {
                        JSONArray topicsArray = responseJson.getJSONArray("topics");
                        List<String> topicIds = new ArrayList<>();
                        if (topicsArray != null) {
                            for (int i = 0; i < topicsArray.length(); i++) {
                                String topicId = topicsArray.getString(i);
                                if (topicId != null) {
                                    topicIds.add(topicId);
                                }
                            }
                        }
                        sharedPreferences.edit().putStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>(topicIds)).apply();

                        if (responseJson.has("topicsVersion")) {
                            int topicsVersion = responseJson.getInt("topicsVersion");
                            if (topicsVersion > 0) {
                                sharedPreferences.edit().putInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, topicsVersion).apply();
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                System.out.println("CleverPush IIDLS failure: " + statusCode + " " + response);
            }
        });
    }
}
