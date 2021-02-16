package com.cleverpush.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

abstract class SubscriptionManagerBase implements SubscriptionManager {
    RegisteredHandler registeredHandler;
    Context context;
    JSONObject channelConfig;

    SubscriptionManagerBase(Context context) {
        this.context = context;
    }

    @Override
    public void subscribe(JSONObject channelConfig, RegisteredHandler callback) {
        this.registeredHandler = callback;
        this.channelConfig = channelConfig;
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

        if (this.getProviderName().equals("ADM")) {
            sharedPreferences.edit().putString(CleverPushPreferences.ADM_TOKEN, token).apply();
        } else if (this.getProviderName().equals("HMS")) {
            sharedPreferences.edit().putString(CleverPushPreferences.HMS_TOKEN, token).apply();
        } else {
            sharedPreferences.edit().putString(CleverPushPreferences.FCM_TOKEN, token).apply();
        }

        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (channelId == null) {
            Log.d("CleverPush", "channelId in preferences not found");
            return;
        }

        String language = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_LANGUAGE, null);
        String country = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_COUNTRY, null);
        TimeZone timeZone = TimeZone.getDefault();

        Set<String> topics = null;
        if (sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_TOPICS)) {
            topics = sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, null);
        }

        int topicsVersion = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, 0) + 1;

        String appVersion = "";
        if (this.context != null) {
            try {
                PackageInfo pInfo = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0);
                appVersion = pInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        JSONObject jsonBody = new JSONObject();
        try {
            if (this.getProviderName().equals("ADM")) {
                jsonBody.put("admToken", token);
            } else if (this.getProviderName().equals("HMS")) {
                jsonBody.put("hmsToken", token);
                jsonBody.put("hmsId", senderId);
            } else {
                jsonBody.put("fcmToken", token);
                jsonBody.put("fcmId", senderId);
            }
            if (subscriptionId != null) {
                jsonBody.put("subscriptionId", subscriptionId);
            }
            jsonBody.put("platformName", "Android");
            jsonBody.put("platformVersion", Build.VERSION.RELEASE);
            jsonBody.put("browserType", "SDK");
            jsonBody.put("browserVersion", CleverPush.SDK_VERSION);
            jsonBody.put("appVersion", appVersion);
            if (language != null) {
                jsonBody.put("language", language);
            }
            if (country != null) {
                jsonBody.put("country", country);
            }
            if (timeZone != null && timeZone.getID() != null) {
                jsonBody.put("timezone", timeZone.getID());
            }
            if (topics != null) {
                jsonBody.put("topics", new JSONArray(topics));
                jsonBody.put("topicsVersion", topicsVersion);
            }
        } catch (JSONException e) {
            Log.e("CleverPush", "Error", e);
        }

        CleverPushHttpClient.post("/subscription/sync/" + channelId, jsonBody, new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("CleverPush", "sync response: " + response);

                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.has("id")) {
                        String newSubscriptionId = responseJson.getString("id");

                        sharedPreferences.edit().putString(CleverPushPreferences.SUBSCRIPTION_ID, newSubscriptionId).apply();
                        sharedPreferences.edit().putInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, (int) (System.currentTimeMillis() / 1000L)).apply();

                        registeredHandler.complete(newSubscriptionId);
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
