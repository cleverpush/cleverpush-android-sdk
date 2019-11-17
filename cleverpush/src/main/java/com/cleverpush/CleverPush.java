package com.cleverpush;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cleverpush.listener.AppBannerUrlOpenedListener;
import com.cleverpush.listener.ChannelAttributesListener;
import com.cleverpush.listener.ChannelTagsListener;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.NotificationReceivedListener;
import com.cleverpush.listener.SubscribedListener;
import com.cleverpush.manager.SubscriptionManager;
import com.cleverpush.manager.SubscriptionManagerADM;
import com.cleverpush.manager.SubscriptionManagerFCM;
import com.cleverpush.manager.SubscriptionManagerGCM;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class CleverPush {

    public static final String SDK_VERSION = "0.1.11";

    private static CleverPush instance;

    public static CleverPush getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new CleverPush(context);
        }
        return instance;
    }

    public static Context context;
    static Context activity;

    private NotificationReceivedListener notificationReceivedListener;
    private NotificationOpenedListener notificationOpenedListener;
    private SubscribedListener subscribedListener;
    private Collection<NotificationOpenedResult> unprocessedOpenedNotifications = new ArrayList<>();

    private String channelId;
    private String subscriptionId = null;
    private JSONObject channelConfig = null;
    private boolean subscriptionInProgress = false;

    private CleverPush(@NonNull Context context) {
        if (context instanceof Application) {
            CleverPush.context = context;
            CleverPush.activity = this.getActivity(context);
        } else {
            CleverPush.context = context.getApplicationContext();
            CleverPush.activity = this.getActivity(context);
        }
    }

    public void init() {
        init(null, null, null, null, true);
    }

    public void init(@Nullable final NotificationReceivedListener notificationReceivedListener) {
        String channelId = MetaDataUtils.getChannelId(CleverPush.context);
        init(channelId, notificationReceivedListener);
    }

    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener) {
        String channelId = MetaDataUtils.getChannelId(CleverPush.context);
        init(channelId, notificationOpenedListener);
    }

    public void init(@Nullable final SubscribedListener subscribedListener) {
        String channelId = MetaDataUtils.getChannelId(CleverPush.context);
        init(channelId, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationReceivedListener notificationReceivedListener) {
        init(channelId, notificationReceivedListener, null,null);
    }

    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener) {
        init(channelId, null, notificationOpenedListener, null);
    }

    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) {
        init(null, null, notificationOpenedListener, subscribedListener);
    }

    public void init(@Nullable final NotificationReceivedListener notificationReceivedListener, @Nullable final SubscribedListener subscribedListener) {
        init(null, notificationReceivedListener, null, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationReceivedListener notificationReceivedListener, @Nullable final NotificationOpenedListener notificationOpenedListener) {
        init(channelId, notificationReceivedListener, notificationOpenedListener, null);
    }

    public void init(String channelId, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, null, null, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationReceivedListener notificationReceivedListener, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, notificationReceivedListener, null, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, null, notificationOpenedListener, subscribedListener);
    }

    public void init(String channelId, @Nullable final NotificationReceivedListener notificationReceivedListener, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, notificationReceivedListener, notificationOpenedListener, subscribedListener, true);
    }

    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
        init(channelId, null, notificationOpenedListener, subscribedListener, autoRegister);
    }

    public void init(String channelId, @Nullable final NotificationReceivedListener notificationReceivedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
        init(channelId, notificationReceivedListener, null, subscribedListener, autoRegister);
    }

    public void init(String channelId, @Nullable final NotificationReceivedListener notificationReceivedListener, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
        this.channelId = channelId;
        this.notificationReceivedListener = notificationReceivedListener;
        this.notificationOpenedListener = notificationOpenedListener;
        this.subscribedListener = subscribedListener;

        SubscriptionManagerFCM.disableFirebaseInstanceIdService(CleverPush.context);

        // try to get cached Channel ID from Shared Preferences
        if (this.channelId == null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
            this.channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        }

        if (this.channelId != null) {
            Log.d("CleverPush", "Initializing with Channel ID: " + this.channelId + " (SDK " + CleverPush.SDK_VERSION + ")");

            // get channel config
            CleverPush instance = this;
            CleverPushHttpClient.get("/channel/" + this.channelId + "/config", new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject responseJson = new JSONObject(response);
                        instance.setChannelConfig(responseJson);

                        instance.initAppReview();
                    } catch (Throwable ex) {
                        Log.e("CleverPush", ex.getMessage(), ex);
                    }
                }

                @Override
                public void onFailure(int statusCode, String response, Throwable throwable) {

                }
            });

            this.subscribeOrSync(autoRegister);
        } else {
            String bundleId = CleverPush.context.getPackageName();
            Log.d("CleverPush", "No Channel ID specified (in AndroidManifest.xml or as firstParameter for init method), fetching config via Package Name: " + bundleId);

            // get channel config
            CleverPush instance = this;
            CleverPushHttpClient.get("/channel-config?bundleId=" + bundleId + "&platformName=Android", new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    try {
                        JSONObject responseJson = new JSONObject(response);
                        instance.setChannelConfig(responseJson);
                        instance.channelId = responseJson.getString("channelId");

                        instance.subscribeOrSync(autoRegister);

                        instance.initAppReview();

                        Log.d("CleverPush", "Got Channel ID via Package Name: " + instance.channelId + " (SDK " + CleverPush.SDK_VERSION + ")");
                    } catch (Throwable ex) {
                        Log.e("CleverPush", ex.getMessage(), ex);
                    }
                }

                @Override
                public void onFailure(int statusCode, String response, Throwable throwable) {
                    Log.e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?", throwable);
                }
            });
        }

        // fire listeners for unprocessed open notifications
        if (this.notificationOpenedListener != null) {
            for (NotificationOpenedResult result : unprocessedOpenedNotifications) {
                fireNotificationOpenedListener(result);
            }
            unprocessedOpenedNotifications.clear();
        }
    }

    private Activity getActivity(Context context) {
        if (context == null) {
            return null;
        } else if (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            } else {
                return getActivity(((ContextWrapper) context).getBaseContext());
            }
        }
        return null;
    }

    private void subscribeOrSync(boolean autoRegister) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        sharedPreferences.edit().putString(CleverPushPreferences.CHANNEL_ID, this.channelId).apply();

        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        int threeDays = 3 * 60 * 60 * 24;
        int lastSync = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0);
        int nextSync = lastSync + threeDays;
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (subscriptionId == null && autoRegister || subscriptionId != null && nextSync < currentTime) {
            this.subscribe(subscriptionId == null);
        } else {
            Date nextSyncDate = new Date(nextSync*1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());
            String formattedDate = sdf.format(nextSyncDate);
            Log.d("CleverPush", "subscribed with ID (next sync at " + formattedDate + "): " + subscriptionId);
            this.fireSubscribedListener(subscriptionId);
            this.setSubscriptionId(subscriptionId);
        }
    }

    private void initAppReview() {
        JSONObject config = this.getChannelConfig();
        if (config != null && config.optBoolean("appReviewEnabled")) {
            try {
                FiveStarsDialog dialog = new FiveStarsDialog(CleverPush.context, config.optString("appReviewEmail"));
                dialog.setRateText(config.optString("appReviewText"))
                        .setTitle(config.optString("appReviewTitle"))
                        .setForceMode(false)
                        .showAfter(config.optInt("appOpens"));
            } catch (Exception ex) {

            }
        }
    }

    public boolean isSubscribed() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_ID);
    }

    public void subscribe() {
        subscribe(false);
    }

    public void subscribe(boolean newSubscription) {
        if (this.subscriptionInProgress) {
            return;
        }
        this.subscriptionInProgress = true;

        SubscriptionManager subscriptionManager = this.getSubscriptionManager();
        subscriptionManager.subscribe(newSubscriptionId -> {
            this.subscriptionInProgress = false;
            Log.d("CleverPush", "subscribed with ID: " + newSubscriptionId);
            this.fireSubscribedListener(newSubscriptionId);
            this.setSubscriptionId(newSubscriptionId);

            if (newSubscriptionId != null && newSubscription) {
                JSONObject config = this.getChannelConfig();
                if (config != null && !config.optBoolean("confirmAlertHideChannelTopics", false)) {
                    JSONArray channelTopics = config.optJSONArray("channelTopics");
                    if (channelTopics != null && channelTopics.length() > 0) {
                        Set<String> topics = this.getSubscriptionTopics();
                        if (topics == null || topics.size() == 0) {
                            Set<String> selectedTopicIds = new HashSet<>();
                            for (int i = 0; i < channelTopics.length(); i++) {
                                JSONObject channelTopic = channelTopics.optJSONObject(i);
                                if (channelTopic != null && !channelTopic.optBoolean("defaultUnchecked")) {
                                    String id = channelTopic.optString("_id");
                                    if (id != null) {
                                        selectedTopicIds.add(id);
                                    }
                                }
                            }
                            if (selectedTopicIds.size() > 0) {
                                this.setSubscriptionTopics(selectedTopicIds.toArray(new String[0]));
                            }
                        }

                        CleverPush.instance.showTopicsDialog();
                    }
                }
            }
        });
    }

    public void unsubscribe() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
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

    public boolean fireNotificationReceivedListener(final NotificationOpenedResult openedResult) {
        if (notificationReceivedListener == null) {
            return false;
        }
        notificationReceivedListener.notificationReceived(openedResult);
        return true;
    }

    public void removeNotificationReceivedListener() {
        notificationReceivedListener = null;
    }

    public boolean fireNotificationOpenedListener(final NotificationOpenedResult openedResult) {
        if (notificationOpenedListener == null) {
            unprocessedOpenedNotifications.add(openedResult);
            return false;
        }
        notificationOpenedListener.notificationOpened(openedResult);
        return true;
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
            subscriptionManager = new SubscriptionManagerADM(CleverPush.context);
        } else if (isFcm) {
            subscriptionManager = new SubscriptionManagerFCM(CleverPush.context);
        } else {
            subscriptionManager = new SubscriptionManagerGCM(CleverPush.context);
        }

        return subscriptionManager;
    }

    public Set<String> getSubscriptionTags() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
    }

    public Set<String> getSubscriptionTopics() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>());
    }

    public Map<String, String> getSubscriptionAttributes() {
        Map<String, String> outputMap = new HashMap<>();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
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
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

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

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
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

    public void setSubscriptionTopics(String[] topicIds) {
        new Thread(() -> {
            String subscriptionId = this.getSubscriptionId();
            if (subscriptionId != null) {
                JSONObject jsonBody = new JSONObject();
                try {
                    JSONArray topicsArray = new JSONArray();
                    for (String topicId : topicIds) {
                        topicsArray.put(topicId);
                    }

                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("topics", topicsArray);
                    jsonBody.put("subscriptionId", subscriptionId);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

                CleverPushHttpClient.post("/subscription/sync/" + this.channelId, jsonBody, new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove(CleverPushPreferences.SUBSCRIPTION_TOPICS).apply();
                        editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>(Arrays.asList(topicIds)));
                        editor.commit();
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                        Log.e("CleverPush", "Error setting topics - HTTP " + statusCode + ": " + response);
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

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
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

    public void trySubscriptionSync() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        int lastSync = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0);
        int nextSync = lastSync + 5; // allow sync every 5s
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (!this.subscriptionInProgress && subscriptionId != null && nextSync < currentTime) {
            this.subscribe(false);
        }
    }

    public void setSubscriptionLanguage(String language) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        String currentLanguage = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_LANGUAGE, null);
        if (currentLanguage == null || language != null && !currentLanguage.equals(language)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CleverPushPreferences.SUBSCRIPTION_LANGUAGE).apply();
            editor.putString(CleverPushPreferences.SUBSCRIPTION_LANGUAGE, language);
            editor.commit();

            this.trySubscriptionSync();
        }
    }

    public void setSubscriptionCountry(String country) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        String currentCountry = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_COUNTRY, null);
        if (currentCountry == null || country != null && !currentCountry.equals(country)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CleverPushPreferences.SUBSCRIPTION_COUNTRY).apply();
            editor.putString(CleverPushPreferences.SUBSCRIPTION_COUNTRY, country);
            editor.commit();

            this.trySubscriptionSync();
        }
    }

    public Set<Notification> getNotifications() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        Set<String> encodedNotifications = sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>());
        Set<Notification> notifications = new HashSet<>();
        Gson gson = new Gson();
        for (String encodedNotification : encodedNotifications) {
            Notification notification = gson.fromJson(encodedNotification, Notification.class);
            notifications.add(notification);
        }
        return notifications;
    }

    public void showAppBanners() {
        showAppBanners(null);
    }

    public void showAppBanners(AppBannerUrlOpenedListener urlOpenedListener) {
        new Thread(() -> {
            CleverPushHttpClient.get("/channel/" +  this.channelId + "/app-banners", new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    try {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                        Set<String> shownAppBanners = sharedPreferences.getStringSet(CleverPushPreferences.SHOWN_APP_BANNERS, new HashSet<>());

                        JSONObject responseJson = new JSONObject(response);
                        if (responseJson.has("banners")) {
                            JSONArray banners = responseJson.getJSONArray("banners");
                            for (int i = 0; i < banners.length(); i++) {
                                JSONObject banner = banners.getJSONObject(i);
                                if (banner != null && (banner.getString("frequency").equals("always")
                                        || banner.getString("frequency").equals("oncePerSession")
                                        || !shownAppBanners.contains(banner.getString("_id")))) {
                                    AppBanner appBanner = new AppBanner(CleverPush.activity,
                                            banner.getString("_id"),
                                            banner.getString("content"), urlOpenedListener);

                                    appBanner.show();

                                    shownAppBanners.add(appBanner.getId());

                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.remove(CleverPushPreferences.SHOWN_APP_BANNERS).apply();
                                    editor.putStringSet(CleverPushPreferences.SHOWN_APP_BANNERS, shownAppBanners);
                                    editor.commit();
                                }
                            }
                        }
                    } catch (Throwable t) {
                        t.printStackTrace();
                    }
                    System.out.println("app banners: " + response);
                }

                @Override
                public void onFailure(int statusCode, String response, Throwable throwable) {
                    Log.e("CleverPush", "Error getting app banners - HTTP " + statusCode);
                }
            });
        }).start();
    }

    public void showTopicsDialog() {
        JSONObject channelConfig = this.getChannelConfig();

        try {
            JSONArray channelTopics = channelConfig.getJSONArray("channelTopics");
            if (channelTopics.length() == 0) {
                Log.w("CleverPush", "CleverPush: showTopicsDialog: No topics found. Create some first in the CleverPush channel settings.");
            }

            boolean[] checkedTopics = new boolean[channelTopics.length()];
            String[] topicNames = new String[channelTopics.length()];
            String[] topicIds = new String[channelTopics.length()];

            Set selectedTopics = this.getSubscriptionTopics();
            for (int i = 0; i < channelTopics.length(); i++) {
                JSONObject topic = channelTopics.getJSONObject(i);
                if (topic != null) {
                    boolean defaultUnchecked = false;
                    try {
                        defaultUnchecked = topic.optBoolean("defaultUnchecked");
                    } catch (Exception ignored) {}
                    String id = topic.getString("_id");
                    topicIds[i] = id;
                    topicNames[i] = topic.getString("name");
                    checkedTopics[i] = (selectedTopics.size() == 0 && !defaultUnchecked) || selectedTopics.contains(id);
                } else {
                    Log.e("CleverPush", "topic is null");
                }
            }

            ((Activity) activity).runOnUiThread(() -> {
                Log.d("CleverPush", "showTopicsDialog activity: " + activity.getClass().getCanonicalName());

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(activity);
                alertBuilder.setMultiChoiceItems(topicNames, checkedTopics, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        checkedTopics[i] = b;
                    }
                });
                alertBuilder.setPositiveButton("Speichern", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Set<String> selectedTopicIds = new HashSet<>();
                        for (int j = 0; j < topicIds.length; j++) {
                            if (checkedTopics[j]) {
                                selectedTopicIds.add(topicIds[j]);
                            }
                        }

                        CleverPush.getInstance(CleverPush.context).setSubscriptionTopics(selectedTopicIds.toArray(new String[0]));

                        dialogInterface.dismiss();
                    }
                });

                AlertDialog alert = alertBuilder.create();
                alert.show();
            });

        } catch (JSONException e) {
            Log.e("CleverPush", "Error getting channel topics " + e.getMessage());
        }
    }

    static void setAppContext(Context newAppContext) {
        context = newAppContext.getApplicationContext();
    }
}
