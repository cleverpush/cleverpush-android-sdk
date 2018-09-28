package com.cleverpush;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.SubscribedListener;
import com.cleverpush.manager.SubscriptionManager;
import com.cleverpush.manager.SubscriptionManagerADM;
import com.cleverpush.manager.SubscriptionManagerFCM;
import com.cleverpush.manager.SubscriptionManagerGCM;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CleverPush {

    public static final String SDK_VERSION = "0.0.5";

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

    private boolean subscriptionIdAvailable = false;
    private String channelId;
    private String subscriptionId;

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
        this.channelId = channelId;
        this.notificationOpenedListener = notificationOpenedListener;
        this.subscribedListener = subscribedListener;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        sharedPreferences.edit().putString(CleverPushPreferences.CHANNEL_ID, channelId).apply();

        SubscriptionManagerFCM.disableFirebaseInstanceIdService(this.context);

        int currentTime = (int) (System.currentTimeMillis() / 1000L);
        int threeDays = 3 * 60 * 60 * 24;
        int lastSync = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0);
        int nextSync = lastSync + threeDays;
        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
        if (subscriptionId == null || nextSync < currentTime) {
            SubscriptionManager subscriptionManager = this.getSubscriptionManager();
            subscriptionManager.subscribe(newSubscriptionId -> {
                Log.d("CleverPush", "subscribed with ID: " + newSubscriptionId);
                this.fireSubscribedListener(newSubscriptionId);
                this.setSubscriptionId(newSubscriptionId);
            });
        } else {
            Date nextSyncDate = new Date(nextSync*1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            String formattedDate = sdf.format(nextSyncDate);
            Log.d("CleverPush", "subscribed with ID (next sync at " + formattedDate + "): " + subscriptionId);
            this.fireSubscribedListener(subscriptionId);
            this.setSubscriptionId(subscriptionId);
        }
    }

    public synchronized String getSubscriptionId() {
        while (!subscriptionIdAvailable) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }
        subscriptionIdAvailable = false;
        notifyAll();
        return subscriptionId;
    }

    public synchronized void setSubscriptionId(String value) {
        while (subscriptionIdAvailable) {
            try {
                wait();
            } catch (InterruptedException e) { }
        }
        subscriptionId = value;
        subscriptionIdAvailable = true;
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

    public void addSubscriptionTag(String tagId) {
        String subscriptionId = this.getSubscriptionId();
        if (subscriptionId != null) {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("tagId", tagId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            CleverPushHttpClient.post("/subscription/tag", jsonBody, null);
        }
    }

    public void removeSubscriptionTag(String tagId) {
        String subscriptionId = this.getSubscriptionId();
        if (subscriptionId != null) {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("tagId", tagId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            CleverPushHttpClient.post("/subscription/untag", jsonBody, null);
        }
    }

    public void setSubscriptionAttribute(String attributeId, String value) {
        String subscriptionId = this.getSubscriptionId();
        if (subscriptionId != null) {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("attributeId", attributeId);
                jsonBody.put("value", value);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            CleverPushHttpClient.post("/subscription/attribute", jsonBody, null);
        }
    }
}
