package com.cleverpush;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cleverpush.listener.ChannelAttributesListener;
import com.cleverpush.listener.ChannelTagsListener;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.SubscribedListener;
import com.cleverpush.manager.SubscriptionManager;
import com.cleverpush.manager.SubscriptionManagerADM;
import com.cleverpush.manager.SubscriptionManagerFCM;
import com.cleverpush.manager.SubscriptionManagerGCM;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class CleverPush {

    public static final String SDK_VERSION = "0.0.11";

    private static CleverPush instance;

    public static CleverPush getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new CleverPush(context);
        }
        return instance;
    }

    private Context context;

    private NotificationOpenedListener notificationOpenedListener;
    private SubscribedListener subscribedListener;

    private String channelId;
    private String subscriptionId = null;
    private JSONObject channelConfig = null;

    private CleverPush(@NonNull Context context) {
        if (context instanceof Application) {
            this.context = context;
        } else {
            this.context = context.getApplicationContext();
        }
    }

    public void init() throws Exception {
        init(null, null, null);
    }

    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) throws Exception {
        init(null, notificationOpenedListener, subscribedListener);
    }

    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener) throws Exception {
        String channelId = MetaDataUtils.getChannelId(this.context);
        if (channelId == null) {
            throw new Exception("Please set up your CLEVERPUSH_CHANNEL_ID in AndroidManifest.xml or as first parameter");
        }
        init(channelId, notificationOpenedListener);
    }

    public void init(@Nullable final SubscribedListener subscribedListener) throws Exception {
        String channelId = MetaDataUtils.getChannelId(this.context);
        if (channelId == null) {
            throw new Exception("Please set up your CLEVERPUSH_CHANNEL_ID in AndroidManifest.xml or as first parameter");
        }
        init(channelId, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener) throws Exception {
        init(channelId, notificationOpenedListener, null);
    }

    public void init(String channelId, @Nullable final SubscribedListener subscribedListener) throws Exception {
        init(channelId, null, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) throws Exception {
        init(channelId, notificationOpenedListener, subscribedListener, true);
    }

    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) throws Exception {
        this.channelId = channelId;
        this.notificationOpenedListener = notificationOpenedListener;
        this.subscribedListener = subscribedListener;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        sharedPreferences.edit().putString(CleverPushPreferences.CHANNEL_ID, channelId).apply();

        SubscriptionManagerFCM.disableFirebaseInstanceIdService(this.context);

        if (this.channelId == null) {
            throw new Exception("CleverPush channel ID not provided");
        }

        // get channel config
        CleverPush instance = this;
        CleverPushHttpClient.get("/channel/" + this.channelId + "/config", new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject responseJson = new JSONObject(response);
                    instance.setChannelConfig(responseJson);
                } catch (Throwable ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {

            }
        });

        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        int threeDays = 3 * 60 * 60 * 24;
        int lastSync = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0);
        int nextSync = lastSync + threeDays;
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (subscriptionId == null && autoRegister || subscriptionId != null && nextSync < currentTime) {
            this.subscribe();
        } else {
            Date nextSyncDate = new Date(nextSync*1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            String formattedDate = sdf.format(nextSyncDate);
            Log.d("CleverPush", "subscribed with ID (next sync at " + formattedDate + "): " + subscriptionId);
            this.fireSubscribedListener(subscriptionId);
            this.setSubscriptionId(subscriptionId);
        }
    }

    public boolean isSubscribed() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        return sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_ID);
    }

    public void subscribe() {
        SubscriptionManager subscriptionManager = this.getSubscriptionManager();
        subscriptionManager.subscribe(newSubscriptionId -> {
            Log.d("CleverPush", "subscribed with ID: " + newSubscriptionId);
            this.fireSubscribedListener(newSubscriptionId);
            this.setSubscriptionId(newSubscriptionId);
        });
    }

    public void unsubscribe() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (subscriptionId != null) {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                Log.e("CleverPush", "Error", e);
            }

            CleverPushHttpClient.post("/subscription/unsubscribe", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    try {
                        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_ID).apply();
                        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC).apply();
                    } catch (Throwable t) {
                        Log.e("CleverPush", "Error", t);
                    }
                }

                @Override
                public void onFailure(int statusCode, String response, Throwable t) {
                    Log.e("CleverPush", "Failed while unsubscribe request - " + statusCode + " - " + response, t);
                }
            });
        }
    }

    public synchronized JSONObject getChannelConfig() {
        while (channelConfig == null) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }
        notifyAll();
        return channelConfig;
    }

    public synchronized void setChannelConfig(JSONObject value) {
        channelConfig = value;
        notifyAll();
    }

    public synchronized String getSubscriptionId() {
        while (subscriptionId == null) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }
        notifyAll();
        return subscriptionId;
    }

    public synchronized void setSubscriptionId(String value) {
        subscriptionId = value;
        notifyAll();
    }

    public void fireNotificationOpenedListener(final NotificationOpenedResult openedResult) {
        if (notificationOpenedListener == null) {
            return;
        }
        notificationOpenedListener.notificationOpened(openedResult);
    }

    public void removeNotificationOpenedListener() {
        notificationOpenedListener = null;
    }

    public void fireSubscribedListener(final String subscriptionId) {
        if (subscribedListener == null) {
            return;
        }
        subscribedListener.subscribed(subscriptionId);
    }

    public void removeSubscribedListener() {
        subscribedListener = null;
    }

    private SubscriptionManager subscriptionManager;
    private SubscriptionManager getSubscriptionManager() {
        if (subscriptionManager != null) {
            return subscriptionManager;
        }

        boolean isAmazon = false;
        try {
            Class.forName("com.amazon.device.messaging.ADM");
            isAmazon = true;
        } catch (ClassNotFoundException ignored) {
        }

        boolean isFcm = false;
        try {
            if (com.google.firebase.messaging.FirebaseMessaging.class != null) {
                isFcm = true;
            }
        } catch (Throwable ignored) {
        }

        if (isAmazon) {
            subscriptionManager = new SubscriptionManagerADM(this.context);
        } else if (isFcm) {
            subscriptionManager = new SubscriptionManagerFCM(this.context);
        } else {
            subscriptionManager = new SubscriptionManagerGCM(this.context);
        }

        return subscriptionManager;
    }

    public Set<String> getSubscriptionTags() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
    }

    public Map<String, String> getSubscriptionAttributes() {
        Map<String, String> outputMap = new HashMap<>();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        try {
            if (sharedPreferences != null) {
                String jsonString = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while(keysItr.hasNext()) {
                    String k = keysItr.next();
                    String v = (String) jsonObject.get(k);
                    outputMap.put(k,v);
                }
            }
        } catch (Exception ex) {
            Log.e("CleverPush", ex.getMessage(), ex);
        }
        return outputMap;
    }

    public boolean hasSubscriptionTag(String tagId) {
        return this.getSubscriptionTags().contains(tagId);
    }

    public String getSubscriptionAttribute(String attributeId) {
        return this.getSubscriptionAttributes().get(attributeId);
    }

    private Set<ChannelTag> getAvailableTagsFromConfig(JSONObject channelConfig) {
        Set<ChannelTag> tags = new HashSet<>();
        if (channelConfig != null && channelConfig.has("channelTags")) {
            try {
                JSONArray tagsArray = channelConfig.getJSONArray("channelTags");
                if (tagsArray != null) {
                    for (int i = 0; i < tagsArray.length(); i++) {
                        JSONObject tagObject = tagsArray.getJSONObject(i);
                        if (tagObject != null) {
                            ChannelTag tag = new ChannelTag(tagObject.getString("_id"), tagObject.getString("name"));
                            tags.add(tag);
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.d("CleverPush", ex.getMessage(), ex);
            }
        }
        return tags;
    }

    /**
     * @deprecated use this method with ChannelTagsListener
     */
    @Deprecated
    public Set<ChannelTag> getAvailableTags() {
        JSONObject channelConfig = this.getChannelConfig();
        return this.getAvailableTagsFromConfig(channelConfig);
    }

    public void getAvailableTags(ChannelTagsListener listener) {
        new Thread(() -> {
            JSONObject channelConfig = this.getChannelConfig();
            listener.ready(this.getAvailableTagsFromConfig(channelConfig));
        }).start();
    }

    private Set<CustomAttribute> getAvailableAttributesFromConfig(JSONObject channelConfig) {
        Set<CustomAttribute> attributes = new HashSet<>();
        if (channelConfig != null && channelConfig.has("customAttributes")) {
            try {
                JSONArray attributesArray = channelConfig.getJSONArray("customAttributes");
                if (attributesArray != null) {
                    for (int i = 0; i < attributesArray.length(); i++) {
                        JSONObject attributeObject = attributesArray.getJSONObject(i);
                        if (attributeObject != null) {
                            CustomAttribute attribute = new CustomAttribute(attributeObject.getString("id"), attributeObject.getString("name"));
                            attributes.add(attribute);
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.d("CleverPush", ex.getMessage(), ex);
            }
        }
        return attributes;
    }

    /**
     * @deprecated use this method with ChannelAttributesListener
     */
    @Deprecated
    public Set<CustomAttribute> getAvailableAttributes() {
        JSONObject channelConfig = this.getChannelConfig();
        return this.getAvailableAttributesFromConfig(channelConfig);
    }

    public void getAvailableAttributes(ChannelAttributesListener listener) {
        new Thread(() -> {
            JSONObject channelConfig = this.getChannelConfig();
            listener.ready(this.getAvailableAttributesFromConfig(channelConfig));
        }).start();
    }

    public void addSubscriptionTag(String tagId) {
        new Thread(() -> {
            String subscriptionId = this.getSubscriptionId();
            if (subscriptionId != null) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("tagId", tagId);
                    jsonBody.put("subscriptionId", subscriptionId);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

                Set<String> tags = this.getSubscriptionTags();
                tags.add(tagId);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);

                CleverPushHttpClient.post("/subscription/tag", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS).apply();
                        editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, tags);
                        editor.commit();
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                        Log.e("CleverPush", "Error adding tag - HTTP " + statusCode);
                    }
                });
            }
        }).start();
    }

    public void removeSubscriptionTag(String tagId) {
        new Thread(() -> {
            String subscriptionId = this.getSubscriptionId();
            if (subscriptionId != null) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("tagId", tagId);
                    jsonBody.put("subscriptionId", subscriptionId);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
                Set<String> tags = this.getSubscriptionTags();
                tags.remove(tagId);

                CleverPushHttpClient.post("/subscription/untag", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS).apply();
                        editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, tags);
                        editor.commit();
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                        Log.e("CleverPush", "Error removing tag - HTTP " + statusCode);
                    }
                });
            }
        }).start();
    }

    public void setSubscriptionAttribute(String attributeId, String value) {
        new Thread(() -> {
            String subscriptionId = this.getSubscriptionId();
            if (subscriptionId != null) {
                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("attributeId", attributeId);
                    jsonBody.put("value", value);
                    jsonBody.put("subscriptionId", subscriptionId);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
                Map<String, String> subscriptionAttributes = this.getSubscriptionAttributes();
                subscriptionAttributes.put(attributeId, value);

                CleverPushHttpClient.post("/subscription/attribute", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        try {
                            if (sharedPreferences != null) {
                                JSONObject jsonObject = new JSONObject(subscriptionAttributes);
                                String jsonString = jsonObject.toString();
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES).apply();
                                editor.putString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, jsonString);
                                editor.commit();
                            }
                        } catch (Exception ex) {
                            Log.e("CleverPush", ex.getMessage(), ex);
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                        Log.e("CleverPush", "Error setting attribute - HTTP " + statusCode);
                    }
                });
            }
        }).start();
    }
}
