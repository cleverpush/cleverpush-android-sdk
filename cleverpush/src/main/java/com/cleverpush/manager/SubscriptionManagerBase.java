package com.cleverpush.manager;

import static com.cleverpush.Constants.LOG_TAG;

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
import com.cleverpush.listener.SubscribedListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

abstract class SubscriptionManagerBase implements SubscriptionManager {
    Context context;
    SubscriptionManagerType type;

    SubscriptionManagerBase(Context context, SubscriptionManagerType type) {
        this.context = context;
        this.type = type;
    }

    void syncSubscription(String token, SubscribedListener subscribedListener) {
        syncSubscription(token, subscribedListener, null);
    }

    void syncSubscription(String token, SubscribedListener subscribedListener, String senderId) {
        syncSubscription(token, subscribedListener, senderId, false);
    }

    void syncSubscription(String token, SubscribedListener subscribedListener, String senderId, boolean isRetry) {
        Log.d(LOG_TAG, "syncSubscription");

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);

        if (this.type == SubscriptionManagerType.ADM) {
            sharedPreferences.edit().putString(CleverPushPreferences.ADM_TOKEN, token).apply();
        } else if (this.type == SubscriptionManagerType.HMS) {
            sharedPreferences.edit().putString(CleverPushPreferences.HMS_TOKEN, token).apply();
        } else {
            sharedPreferences.edit().putString(CleverPushPreferences.FCM_TOKEN, token).apply();
        }

        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (channelId == null) {
            Log.d(LOG_TAG, "channelId in preferences not found");
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
            if (this.type == SubscriptionManagerType.ADM) {
                jsonBody.put("admToken", token);
            } else if (this.type == SubscriptionManagerType.HMS) {
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
            Log.e(LOG_TAG, "Error", e);
        }

        CleverPushHttpClient.post("/subscription/sync/" + channelId, jsonBody, new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d(LOG_TAG, "sync response: " + response);

                    JSONObject responseJson = new JSONObject(response);
                    if (responseJson.has("id")) {
                        String newSubscriptionId = responseJson.getString("id");

                        sharedPreferences.edit().putString(CleverPushPreferences.SUBSCRIPTION_ID, newSubscriptionId).apply();
                        sharedPreferences.edit().putInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, (int) (System.currentTimeMillis() / 1000L)).apply();

                        subscribedListener.subscribed(newSubscriptionId);
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
                        if (!CleverPush.isSubscribeForTopicsDialog()) {
                            sharedPreferences.edit().putStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>(topicIds)).apply();
                        } else {
                            CleverPush.setIsSubscribeForTopicsDialog(false);
                        }

                        if (responseJson.has("topicsVersion")) {
                            int topicsVersion = responseJson.getInt("topicsVersion");
                            if (topicsVersion > 0) {
                                sharedPreferences.edit().putInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, topicsVersion).apply();
                            }
                        }
                    }
                } catch (Throwable t) {
                    Log.e(LOG_TAG, "Error", t);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable t) {
                if ((statusCode == 404 || statusCode == 410) && !isRetry) {
                    sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_ID).apply();
                    syncSubscription(token, subscribedListener, senderId, true);
                    return;
                }
                Log.e(LOG_TAG, "Failed while sync subscription request - " + statusCode + " - " + response, t);
            }
        });
    }

    public SubscriptionManagerType getType() {
        return this.type;
    }

    @Override
    public void checkChangedPushToken(JSONObject channelConfig) {
        this.checkChangedPushToken(channelConfig, null);
    }
}
