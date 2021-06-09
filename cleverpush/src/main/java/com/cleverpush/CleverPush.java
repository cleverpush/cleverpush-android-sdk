package com.cleverpush;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cleverpush.banner.AppBannerModule;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.listener.ChannelAttributesListener;
import com.cleverpush.listener.ChannelConfigListener;
import com.cleverpush.listener.ChannelTagsListener;
import com.cleverpush.listener.ChannelTopicsListener;
import com.cleverpush.listener.ChatSubscribeListener;
import com.cleverpush.listener.ChatUrlOpenedListener;
import com.cleverpush.listener.CompletionListener;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.NotificationReceivedCallbackListener;
import com.cleverpush.listener.NotificationReceivedListenerBase;
import com.cleverpush.listener.SessionListener;
import com.cleverpush.listener.SubscribedListener;
import com.cleverpush.listener.TopicsChangedListener;
import com.cleverpush.listener.TopicsDialogListener;
import com.cleverpush.listener.TrackingConsentListener;
import com.cleverpush.manager.SubscriptionManager;
import com.cleverpush.manager.SubscriptionManagerADM;
import com.cleverpush.manager.SubscriptionManagerFCM;
import com.cleverpush.manager.SubscriptionManagerHMS;
import com.cleverpush.service.CleverPushGeofenceTransitionsIntentService;
import com.cleverpush.service.TagsMatcher;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.huawei.hms.api.HuaweiApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class CleverPush implements  ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String SDK_VERSION = "1.14.2";

    private static CleverPush instance;

    public static CleverPush getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new CleverPush(context);
        }
        return instance;
    }

    public static Context context;

    private NotificationReceivedListenerBase notificationReceivedListener;
    private NotificationOpenedListener notificationOpenedListener;
    private SubscribedListener subscribedListener;
    private ChatUrlOpenedListener chatUrlOpenedListener;
    private ChatSubscribeListener chatSubscribeListener;
    private TopicsChangedListener topicsChangedListener;
    private AppBannerOpenedListener appBannerOpenedListener;
    private Collection<SubscribedListener> getSubscriptionIdListeners = new ArrayList<>();
    private Collection<ChannelConfigListener> getChannelConfigListeners = new ArrayList<>();
    private Collection<NotificationOpenedResult> unprocessedOpenedNotifications = new ArrayList<>();
    private SessionListener sessionListener;
    private GoogleApiClient googleApiClient;
    private ArrayList<Geofence> geofenceList = new ArrayList<>();
    private Map<String, Boolean> autoAssignSessionsCounted = new HashMap<>();
    private Map<String, String> pendingAppBannerEvents = new HashMap<>();
    private String pendingShowAppBannerId = null;
	private String pendingShowAppBannerNotificationId = null;
    private String currentPageUrl;
    private AppBannerModule appBannerModule;
	private boolean appBannersDisabled = false;

    private String channelId;
    private String subscriptionId = null;
    private JSONObject channelConfig = null;
    private boolean subscriptionInProgress = false;
    private boolean initialized = false;
    private int brandingColor;
    private boolean pendingRequestLocationPermissionCall = false;
    private boolean pendingInitFeaturesCall = false;
    private ArrayList<PageView> pendingPageViews = new ArrayList<>();

    private int sessionVisits = 0;
    private long sessionStartedTimestamp = 0;
    private int locationPermissionRequestCode = 101;

    private boolean trackingConsentRequired = false;
    private boolean hasTrackingConsent = false;
    private boolean hasTrackingConsentCalled = false;
    private Collection<TrackingConsentListener> trackingConsentListeners = new ArrayList<>();

    private boolean incrementBadge = false;
    private boolean autoClearBadge = false;

    private boolean developmentMode = false;

    private boolean showingTopicsDialog = false;

    private CleverPush(@NonNull Context context) {
        if (context == null) {
            return;
        }

        if (context instanceof Application) {
            CleverPush.context = context;
        } else {
            CleverPush.context = context.getApplicationContext();
        }

          sessionListener = open -> {
            if (open) {
                this.trackSessionStart();

                if (pendingRequestLocationPermissionCall) {
                    this.requestLocationPermission();
                }

                if (pendingInitFeaturesCall) {
                    this.initFeatures();
                }

                if (appBannerModule != null) {
                    appBannerModule.initSession(channelId);
                }

                if (pendingPageViews.size() > 0) {
                    for (PageView pageView : pendingPageViews) {
                        this.trackPageView(pageView.getUrl(), pageView.getParams());
                    }
                    pendingPageViews = new ArrayList<>();
                }
            } else {
                this.trackSessionEnd();
            }
        };

        ActivityLifecycleListener.registerActivityLifecycleCallbacks((Application) CleverPush.context, sessionListener);
    }

    /**
     *initialize Cleverpush SDK
     */
    public void init() {
        init(null, null, null, null, true);
    }

    /**
     *initialize Cleverpush SDK with notification received callback
     * @param notificationReceivedListener callback for the notification received
     */
    public void init(@Nullable final NotificationReceivedListenerBase notificationReceivedListener) {
        String channelId = MetaDataUtils.getChannelId(CleverPush.context);
        init(channelId, notificationReceivedListener);
    }

    /**
     *initialize Cleverpush SDK with notification opened callback
     * @param notificationOpenedListener callback for the notification opened
     */
    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener) {
        String channelId = MetaDataUtils.getChannelId(CleverPush.context);
        init(channelId, notificationOpenedListener);
    }
    /**
     *initialize Cleverpush SDK with subscribed callback
     * @param subscribedListener callback for subscription
     */

    public void init(@Nullable final SubscribedListener subscribedListener) {
        String channelId = MetaDataUtils.getChannelId(CleverPush.context);
        init(channelId, subscribedListener);
    }
    /**
     *initialize Cleverpush SDK for channel
     * @param channelId channelID of the channel
     */
    public void init(String channelId) {
        init(channelId, null, null, null);
    }

    /**
     *initialize Cleverpush SDK for channel with notification received callback
     * @param channelId channelID of the channel
     * @param notificationReceivedListener callback for the notification received
     */
    public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener) {
        init(channelId, notificationReceivedListener, null, null);
    }

    /**
     *initialize Cleverpush SDK for channel with notification opened callback
     * @param channelId channelID of the channel
     * @param notificationOpenedListener callback for the notification opened
     */
    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener) {
        init(channelId, null, notificationOpenedListener, null);
    }

    /**
     *initialize Cleverpush SDK with notification opened callback and subscribed callback
     * @param notificationOpenedListener callback for the notification opened
     * @param subscribedListener callback for subscription
     */
    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) {
        init(null, null, notificationOpenedListener, subscribedListener);
    }

    /**
     *initialize Cleverpush SDK with notification received callback and subscribed callback
     * @param notificationReceivedListener callback for the notification received
     * @param subscribedListener callback for subscription
     */
    public void init(@Nullable final NotificationReceivedListenerBase notificationReceivedListener, @Nullable final SubscribedListener subscribedListener) {
        init(null, notificationReceivedListener, null, subscribedListener);
    }

    /**
     *initialize Cleverpush SDK for channel with notification received callback and notification opened callback
     * @param channelId channelID of the channel
     * @param notificationReceivedListener callback for the notification received
     * @param notificationOpenedListener callback for the notification opened
     */
    public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener, @Nullable final NotificationOpenedListener notificationOpenedListener) {
        init(channelId, notificationReceivedListener, notificationOpenedListener, null);
    }

    /**
     *initialize Cleverpush SDK for channel with subscribed callback
     * @param channelId channelID of the channel
     * @param subscribedListener callback for subscription
     */
    public void init(String channelId, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, null, null, subscribedListener);
    }

    /**
     *initialize Cleverpush SDK for channel with notification received callback and subscribed callback
     * @param channelId channelID of the channel
     * @param notificationReceivedListener callback for the notification received
     * @param subscribedListener callback for subscription
     */
    public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, notificationReceivedListener, null, subscribedListener);
    }

    /**
     *initialize Cleverpush SDK for channel with notification opened callback and subscribed callback
     * @param channelId channelID of the channel
     * @param notificationOpenedListener callback for the notification opened
     * @param subscribedListener callback for subscription
     */
    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, null, notificationOpenedListener, subscribedListener);
    }

    /**
     *initialize Cleverpush SDK for channel with notification received, notification opened callback and subscribed callback
     * @param channelId channelID of the channel
     * @param notificationReceivedListener callback for the notification received
     * @param notificationOpenedListener callback for the notification opened
     * @param subscribedListener callback for subscription
     */
    public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener) {
        init(channelId, notificationReceivedListener, notificationOpenedListener, subscribedListener, true);
    }

    /**
     *initialize Cleverpush SDK for channel with notification opened callback and subscribed callback and if there is autoRegister
     * @param channelId channelID of the channel
     * @param notificationOpenedListener callback for the notification opened
     * @param subscribedListener callback for subscription
     * @param autoRegister boolean for auto register
     */
    public void init(String channelId, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
        init(channelId, null, notificationOpenedListener, subscribedListener, autoRegister);
    }

    /**
     *initialize Cleverpush SDK for channel with notification received callback and subscribed callback and if there is autoRegister
     * @param channelId channelID of the channel
     * @param notificationReceivedListener callback for the notification received
     * @param subscribedListener callback for subscription
     * @param autoRegister boolean for auto register
     */
    public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
        init(channelId, notificationReceivedListener, null, subscribedListener, autoRegister);
    }
    /**
     *initialize Cleverpush SDK for channel with notification received callback, notification opened and subscribed callback and if there is autoRegister
     * @param channelId channelID of the channel
     * @param notificationReceivedListener callback for the notification received
     * @param notificationOpenedListener callback for the notification opened
     * @param subscribedListener callback for subscription
     * @param autoRegister boolean for auto register
     */
    public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener, @Nullable final NotificationOpenedListener notificationOpenedListener, @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
        this.channelId = channelId;
        this.notificationReceivedListener = notificationReceivedListener;
        this.notificationOpenedListener = notificationOpenedListener;
        this.subscribedListener = subscribedListener;

        channelConfig = null;

        SubscriptionManagerFCM.disableFirebaseInstanceIdService(CleverPush.context);

        // try to get cached Channel ID from Shared Preferences
        if (this.channelId == null) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
            this.channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
        }

        if (this.channelId != null) {
            Log.d("CleverPush", "Initializing with Channel ID: " + this.channelId + " (SDK " + CleverPush.SDK_VERSION + ")");

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

            String storedChannelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);
            String storedSubscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);

            // Check if the channel id changed. Remove the Subscription ID in this case.
            // Maybe the user switched from Dev to Live environment.
            boolean channelIdChanged = storedSubscriptionId != null && storedChannelId != null && !this.channelId.equals(storedChannelId);
            if (channelIdChanged) {
                try {
                    this.clearSubscriptionData();
                } catch (Throwable t) {
                    Log.e("CleverPush", "Error", t);
                }
            }

            sharedPreferences.edit().putString(CleverPushPreferences.CHANNEL_ID, this.channelId).apply();

            // get channel config
            CleverPush instance = this;
            String configPath = "/channel/" + this.channelId + "/config";
            if (developmentMode) {
                configPath += "?t=" + System.currentTimeMillis();
            }
            CleverPushHttpClient.get(configPath, new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    initialized = true;

                    try {
                        JSONObject responseJson = new JSONObject(response);
                        instance.setChannelConfig(responseJson);

                        instance.subscribeOrSync(autoRegister || channelIdChanged);

                        instance.initFeatures();

                    } catch (Throwable ex) {
                        Log.e("CleverPush", ex.getMessage(), ex);
                    }
                }

                @Override
                public void onFailure(int statusCode, String response, Throwable throwable) {
                    initialized = true;

                    Log.e("CleverPush", "Failed to fetch Channel Config", throwable);

                    // trigger listeners
                    if (channelConfig == null) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
                        instance.fireSubscribedListener(subscriptionId);
                        instance.setSubscriptionId(subscriptionId);
                        instance.setChannelConfig(null);
                    }
                }
            });
        } else {
            String bundleId = CleverPush.context.getPackageName();
            Log.d("CleverPush", "No Channel ID specified (in AndroidManifest.xml or as firstParameter for init method), fetching config via Package Name: " + bundleId);

            // get channel config
            CleverPush instance = this;
            CleverPushHttpClient.get("/channel-config?bundleId=" + bundleId + "&platformName=Android", new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    initialized = true;

                    try {
                        JSONObject responseJson = new JSONObject(response);
                        instance.setChannelConfig(responseJson);
                        instance.channelId = responseJson.getString("channelId");

                        instance.subscribeOrSync(autoRegister);

                        instance.initFeatures();

                        Log.d("CleverPush", "Got Channel ID via Package Name: " + instance.channelId + " (SDK " + CleverPush.SDK_VERSION + ")");
                    } catch (Throwable ex) {
                        Log.e("CleverPush", ex.getMessage(), ex);
                    }
                }

                @Override
                public void onFailure(int statusCode, String response, Throwable throwable) {
                    initialized = true;

                    Log.e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?", throwable);

                    // trigger listeners
                    if (channelConfig == null) {
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                        String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
                        instance.fireSubscribedListener(subscriptionId);
                        instance.setSubscriptionId(subscriptionId);
                        instance.setChannelConfig(null);
                    }
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

        // increment app opens
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        int appOpens = sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 0) + 1;
        editor.putInt(CleverPushPreferences.APP_OPENS, appOpens);
        editor.apply();
    }

    /**
     * check if initialized
     */
    public boolean isInitialized() {
        return this.channelId != null && this.channelConfig != null;
    }

    /**
     * subscribe or sync subscription
     * @param autoRegister boolean for auto register
     */
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
            Date nextSyncDate = new Date(nextSync * 1000L);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault());
            String formattedDate = sdf.format(nextSyncDate);
            Log.d("CleverPush", "Subscribed with ID (next sync at " + formattedDate + "): " + subscriptionId);
            this.fireSubscribedListener(subscriptionId);
            this.setSubscriptionId(subscriptionId);
        }
    }

    /**
     * initialize the features
     */
    private void initFeatures() {
        if (ActivityLifecycleListener.currentActivity == null) {
            this.pendingInitFeaturesCall = true;
            return;
        }
        this.pendingInitFeaturesCall = false;

        this.showPendingTopicsDialog();
        this.initAppReview();
        this.initGeoFences();

        appBannerModule = AppBannerModule.init(ActivityLifecycleListener.currentActivity, channelId, this.developmentMode);

        if (pendingAppBannerEvents != null) {
            for (Map.Entry<String, String> entry : pendingAppBannerEvents.entrySet()) {
                appBannerModule.triggerEvent(entry.getKey(), entry.getValue());
            }
            pendingAppBannerEvents = null;
        }

        if (pendingShowAppBannerId != null) {
            appBannerModule.showBannerById(pendingShowAppBannerId, pendingShowAppBannerNotificationId);
            pendingShowAppBannerId = null;
			pendingShowAppBannerNotificationId = null;
        }

        appBannerModule.initSession(channelId);
    }

    /**
     * initialize the App review
     */
    private void initAppReview() {
        this.getChannelConfig(config -> {
            if (config != null && config.optBoolean("appReviewEnabled")) {
                try {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

                    if (sharedPreferences.getLong(CleverPushPreferences.APP_REVIEW_SHOWN, 0) == 0) {
                        int appReviewSeconds = config.optInt("appReviewSeconds", 0);
                        int appReviewOpens = config.optInt("appReviewOpens", 0);
                        int appReviewDays = config.optInt("appReviewDays", 0);

                        long currentUnixTime = System.currentTimeMillis() / 1000L;
                        long allowedUnixTime = sharedPreferences.getLong(CleverPushPreferences.SUBSCRIPTION_CREATED_AT, 0) + (appReviewDays * 60 * 60 * 24);
                        int appOpens = sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 1);

                        if (currentUnixTime >= allowedUnixTime && appOpens >= appReviewOpens) {
                            (ActivityLifecycleListener.currentActivity).runOnUiThread(() -> {
                                new Handler().postDelayed(() -> {
                                    if (sharedPreferences.getLong(CleverPushPreferences.APP_REVIEW_SHOWN, 0) == 0) {
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.putLong(CleverPushPreferences.APP_REVIEW_SHOWN, System.currentTimeMillis() / 1000L);
                                        editor.apply();

                                        FiveStarsDialog dialog = new FiveStarsDialog(
                                                ActivityLifecycleListener.currentActivity,
                                                config.optString("appReviewEmail")
                                        );
                                        dialog.setRateText(config.optString("appReviewText"))
                                                .setTitle(config.optString("appReviewTitle"))
                                                .setPositiveButtonText(config.optString("appReviewYes"))
                                                .setNegativeButtonText(config.optString("appReviewLater"))
                                                .setNeverButtonText(config.optString("appReviewNo"))
                                                .setForceMode(false)
                                                .show();
                                    }
                                }, appReviewSeconds * 1000);
                            });
                        }
                    }
                } catch (Exception ex) {
                    Log.d("CleverPush", ex.getMessage());
                }
            }
        });
    }

    /**
     * request for location permission
     */
    public void requestLocationPermission() {
        if (this.hasLocationPermission()) {
            return;
        }
        if (ActivityLifecycleListener.currentActivity == null) {
            this.pendingRequestLocationPermissionCall = true;
            return;
        }
        this.pendingRequestLocationPermissionCall = false;
        ActivityCompat.requestPermissions(ActivityLifecycleListener.currentActivity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, locationPermissionRequestCode);
    }

    /**
     * to check if app has location permission
     */
    public boolean hasLocationPermission() {
        /*
        if (android.os.Build.VERSION.SDK_INT >= 29 && ContextCompat.checkSelfPermission(CleverPush.context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        */
        return ContextCompat.checkSelfPermission(CleverPush.context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }


    private void savePreferencesMap(String mapKey, Map<String, Integer> inputMap) {
        Log.d("CleverPush", "savePreferencesMap: " + mapKey + " - " + inputMap.toString());
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        JSONObject jsonObject = new JSONObject(inputMap);
        String jsonString = jsonObject.toString();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(mapKey).apply();
        editor.putString(mapKey, jsonString).apply();
    }

    private Map<String, Integer> loadPreferencesMap(String mapKey) {
        Map<String, Integer> outputMap = new HashMap<>();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        try {
            String jsonString = sharedPreferences.getString(mapKey, (new JSONObject()).toString());
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> keysItr = jsonObject.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Integer value = jsonObject.getInt(key);
                outputMap.put(key, value);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.d("CleverPush", "loadPreferencesMap: " + mapKey + " - " + outputMap.toString());
        return outputMap;
    }

    private void checkTags(String urlStr, Map<String, ?> params) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

        try {
            URL url = new URL(urlStr);
            String pathname = url.getPath();
            Log.d("CleverPush", "checkTags: " + pathname);

            this.getAvailableTags(tags -> {
                if (tags != null) {
                    CleverPush self = this;
                    for (ChannelTag tag : tags) {
                        TagsMatcher.autoAssignTagMatches(tag, pathname, params, matches -> {
                            if (matches) {
                                Log.d("CleverPush", "checkTag: matches: YES - " + tag.getName());

                                String visitsStorageKey = "cleverpush-tag-autoAssignVisits-" + tag.getId();
                                String sessionsStorageKey = "cleverpush-tag-autoAssignSessions-" + tag.getId();

                                int visitsNeeded = Math.max(tag.getAutoAssignVisits(), 0);

                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                                String dateKey = dateFormat.format(Calendar.getInstance().getTime());
                                Date dateAfter = null;
                                if (tag.getAutoAssignDays() > 0) {
                                    dateAfter = new Date();
                                    dateAfter.setDate(dateAfter.getDate() - tag.autoAssignDays);
                                }

                                int visits = 0;
                                Map<String, Integer> dailyVisits = new HashMap<>();
                                if (tag.getAutoAssignDays() > 0 && dateAfter != null) {
                                    try {
                                        dailyVisits = self.loadPreferencesMap(visitsStorageKey);

                                        for (Map.Entry<String, Integer> entry : dailyVisits.entrySet()) {
                                            String curDateKey = entry.getKey();
                                            Integer value = entry.getValue();
                                            Date curDate = dateFormat.parse(curDateKey);
                                            if (curDate.equals(dateAfter) || curDate.after(dateAfter)) {
                                                visits += value;
                                            } else {
                                                dailyVisits.remove(curDateKey);
                                            }
                                        }
                                    } catch (Exception err) {
                                        dailyVisits = new HashMap<>();
                                    }
                                } else {
                                    visits = sharedPreferences.getInt(visitsStorageKey, 0);
                                }

                                int sessionsNeeded = Math.max(tag.getAutoAssignSessions(), 0);


                                int sessions = 0;
                                Map<String, Integer> dailySessions = new HashMap<>();
                                if (tag.getAutoAssignDays() > 0 && dateAfter != null) {
                                    try {
                                        dailySessions = self.loadPreferencesMap(sessionsStorageKey);

                                        for (Map.Entry<String, Integer> entry : dailySessions.entrySet()) {
                                            String curDateKey = entry.getKey();
                                            Integer value = entry.getValue();
                                            Date curDate = dateFormat.parse(curDateKey);
                                            if (curDate.equals(dateAfter) || curDate.after(dateAfter)) {
                                                sessions += value;
                                            } else {
                                                dailySessions.remove(curDateKey);
                                            }
                                        }
                                    } catch (Exception err) {
                                        dailySessions = new HashMap<>();
                                    }
                                } else {
                                    sessions = sharedPreferences.getInt(sessionsStorageKey, 0);
                                }

                                if (sessions >= sessionsNeeded) {
                                    if (visits >= visitsNeeded) {
                                        if (tag.getAutoAssignSeconds() > 0) {
                                            new Timer().schedule(new TimerTask() {
                                                @Override
                                                public void run() {
                                                    if (self.currentPageUrl.equals(urlStr)) {
                                                        self.addSubscriptionTag(tag.getId());
                                                    }
                                                }
                                            }, tag.getAutoAssignSeconds() * 1000L);
                                        } else {
                                            self.addSubscriptionTag(tag.getId());
                                        }
                                    } else {
                                        if (tag.getAutoAssignDays() > 0) {
                                            int dateVisits = 0;
                                            Integer currVisits = dailyVisits.get(dateKey);
                                            if (currVisits != null) {
                                                dateVisits = currVisits;
                                            }
                                            dateVisits += 1;
                                            dailyVisits.put(dateKey, dateVisits);

                                            self.savePreferencesMap(visitsStorageKey, dailyVisits);
                                        } else {
                                            visits += 1;
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.remove(visitsStorageKey).apply();
                                            editor.putInt(visitsStorageKey, visits).apply();
                                        }
                                    }
                                } else {
                                    if (tag.getAutoAssignDays() > 0) {
                                        int dateVisits = 0;
                                        Integer currVisits = dailyVisits.get(dateKey);
                                        if (currVisits != null) {
                                            dateVisits = currVisits;
                                        }
                                        dateVisits += 1;
                                        dailyVisits.put(dateKey, dateVisits);

                                        self.savePreferencesMap(visitsStorageKey, dailyVisits);

                                        if (!autoAssignSessionsCounted.containsKey(tag.getId())) {
                                            int dateSessions = 0;
                                            Integer currSessions = dailySessions.get(dateKey);
                                            if (currSessions != null) {
                                                dateSessions = currSessions;
                                            }
                                            dateSessions += 1;
                                            dailySessions.put(dateKey, dateSessions);

                                            autoAssignSessionsCounted.put(tag.getId(), true);
                                            self.savePreferencesMap(sessionsStorageKey, dailySessions);
                                        }
                                    } else {
                                        visits += 1;
                                        SharedPreferences.Editor editor = sharedPreferences.edit();
                                        editor.remove(visitsStorageKey).apply();
                                        editor.putInt(visitsStorageKey, visits).apply();

                                        if (!autoAssignSessionsCounted.containsKey(tag.getId())) {
                                            sessions += 1;
                                            editor.remove(sessionsStorageKey).apply();
                                            editor.putInt(sessionsStorageKey, sessions).apply();

                                            autoAssignSessionsCounted.put(tag.getId(), true);
                                        }
                                    }
                                }
                            } else {
                                Log.d("CleverPush", "checkTag: matches: NO - " + tag.getName());
                            }
                        });
                    }
                }
            });
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        ;
    }

    /**
     * initialize Geo Fences
     */
    private void initGeoFences() {
        if (hasLocationPermission()) {
            googleApiClient = new GoogleApiClient.Builder(CleverPush.context)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(@Nullable Bundle bundle) {
                            Log.d("CleverPush", "GoogleApiClient onConnected");

                            if (geofenceList.size() > 0) {
                                Log.d("CleverPush", "initing geofences " + geofenceList.toString());

                                GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
                                builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
                                builder.addGeofences(geofenceList);
                                GeofencingRequest geofenceRequest = builder.build();

                                Intent geofenceIntent = new Intent(CleverPush.context, CleverPushGeofenceTransitionsIntentService.class);
                                // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling addgeoFences()
                                PendingIntent geofencePendingIntent = PendingIntent.getService(CleverPush.context, 0, geofenceIntent, PendingIntent.FLAG_UPDATE_CURRENT);

                                try {
                                    LocationServices.GeofencingApi.addGeofences(
                                            googleApiClient,
                                            geofenceRequest,
                                            geofencePendingIntent
                                    );
                                } catch (SecurityException securityException) {
                                    // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
                                }
                            }
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            Log.d("CleverPush", "GoogleApiClient onConnectionSuspended");
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                            Log.d("CleverPush", "GoogleApiClient onConnectionFailed");
                        }
                    })
                    .addApi(LocationServices.API)
                    .build();

            this.getChannelConfig(config -> {
                if (config != null) {
                    try {
                        JSONArray geoFenceArray = config.getJSONArray("geoFences");
                        if (geoFenceArray != null) {
                            for (int i = 0; i < geoFenceArray.length(); i++) {
                                JSONObject geoFence = geoFenceArray.getJSONObject(i);
                                if (geoFence != null) {
                                    geofenceList.add(new Geofence.Builder()
                                            .setRequestId(geoFence.getString("_id"))
                                            .setCircularRegion(
                                                    geoFence.getDouble("latitude"),
                                                    geoFence.getDouble("longitude"),
                                                    geoFence.getLong("radius")
                                            )
                                            .setExpirationDuration(Geofence.NEVER_EXPIRE) // Future: use "endsAt" instead
                                            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                                            .build());
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.d("CleverPush", ex.getMessage());
                    }
                }
            });

            if (geofenceList.size() > 0) {
                googleApiClient.connect();
            }
        }
    }

    public void trackPageView(String url) {
        this.trackPageView(url, null);
    }

    public void trackPageView(String url, Map<String, ?> params) {
        if (ActivityLifecycleListener.currentActivity == null) {
            this.pendingPageViews.add(new PageView(url, params));
            return;
        }

        this.currentPageUrl = url;

        this.checkTags(url, params);
    }

    private void trackSessionStart() {
        // reset
        this.sessionVisits = 0;
        this.sessionStartedTimestamp = System.currentTimeMillis() / 1000L;

        this.waitForTrackingConsent(() -> this.getChannelConfig(config -> {
            if (config != null && config.optBoolean("trackAppStatistics") || subscriptionId != null) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                String fcmToken = sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null);
                String lastNotificationId = sharedPreferences.getString(CleverPushPreferences.LAST_NOTIFICATION_ID, null);

                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("subscriptionId", subscriptionId);
                    jsonBody.put("fcmToken", fcmToken);
                    jsonBody.put("lastNotificationId", lastNotificationId);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

                CleverPushHttpClient.post("/subscription/session/start", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d("CleverPush", "Session started");
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                        Log.e("CleverPush", "Error setting topics - HTTP " + statusCode + ": " + response);
                    }
                });
            }
        }));
    }

    public void increaseSessionVisits() {
        this.sessionVisits += 1;
    }

    private void trackSessionEnd() {
        if (sessionStartedTimestamp == 0) {
            Log.e("CleverPush", "Error tracking session end - session started timestamp is 0");
            return;
        }

        long sessionEndedTimestamp = System.currentTimeMillis() / 1000L;
        long sessionDuration = sessionEndedTimestamp - sessionStartedTimestamp;

        this.waitForTrackingConsent(() -> this.getChannelConfig(config -> {
            if (config != null && config.optBoolean("trackAppStatistics") || subscriptionId != null) {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                String fcmToken = sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null);

                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("subscriptionId", subscriptionId);
                    jsonBody.put("fcmToken", fcmToken);
                    jsonBody.put("visits", sessionVisits);
                    jsonBody.put("duration", sessionDuration);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

                CleverPushHttpClient.post("/subscription/session/end", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                    @Override
                    public void onSuccess(String response) {
                        Log.d("CleverPush", "Session ended");
                    }

                    @Override
                    public void onFailure(int statusCode, String response, Throwable throwable) {
                        Log.e("CleverPush", "Error setting topics - HTTP " + statusCode + ": " + response);
                    }
                });
            }

            // reset
            this.sessionStartedTimestamp = 0;
            this.sessionVisits = 0;
        }));
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

        this.getChannelConfig(config -> {
            SubscriptionManager subscriptionManager = this.getSubscriptionManager();
            subscriptionManager.subscribe(config, newSubscriptionId -> {
                this.subscriptionInProgress = false;
                Log.d("CleverPush", "subscribed with ID: " + newSubscriptionId);
                this.fireSubscribedListener(newSubscriptionId);
                this.setSubscriptionId(newSubscriptionId);

                if (newSubscriptionId != null && newSubscription) {
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
                                this.setSubscriptionTopics(selectedTopicIds.toArray(new String[0]));
                            }

                            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, true);
                            editor.commit();

                            CleverPush.instance.showPendingTopicsDialog();
                        }
                    }
                }
            });
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

            CleverPush self = this;
            CleverPushHttpClient.post("/subscription/unsubscribe", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                @Override
                public void onSuccess(String response) {
                    try {
                    	Log.d("CleverPush", "unsubscribe success");
                        self.clearSubscriptionData();
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

    public void setTrackingConsentRequired(Boolean required) {
        trackingConsentRequired = required;
    }

    public void waitForTrackingConsent(TrackingConsentListener listener) {
        if (listener != null) {
            if (trackingConsentRequired && !hasTrackingConsent) {
                if (!hasTrackingConsentCalled) {
                    trackingConsentListeners.add(listener);
                }
            } else {
                listener.ready();
            }
        }
    }

    public void setTrackingConsent(Boolean consent) {
        hasTrackingConsentCalled = true;
        hasTrackingConsent = consent;

        if (hasTrackingConsent) {
            for (TrackingConsentListener listener : trackingConsentListeners) {
                listener.ready();
            }
        }
        trackingConsentListeners = new ArrayList<>();
    }

    /**
     * @deprecated use this method with ChannelConfigListener
     */
    @Deprecated
    public synchronized JSONObject getChannelConfig() {
        while (channelConfig == null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        notifyAll();
        return channelConfig;
    }

    public void getChannelConfig(ChannelConfigListener listener) {
        if (listener != null) {
            if (channelConfig == null && !initialized) {
                getChannelConfigListeners.add(listener);
            } else {
                listener.ready(channelConfig);
            }
        }
    }

    public synchronized void setChannelConfig(JSONObject value) {
        channelConfig = value;
        notifyAll();

        if (channelConfig != null || initialized) {
            for (ChannelConfigListener listener : getChannelConfigListeners) {
                listener.ready(channelConfig);
            }
            getChannelConfigListeners = new ArrayList<>();
        }
    }

    /**
     * @deprecated use this method with SubscribedListener
     */
    @Deprecated
    public synchronized String getSubscriptionId() {
        while (subscriptionId == null) {
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        notifyAll();
        return subscriptionId;
    }

    public void getSubscriptionId(SubscribedListener listener) {
        if (listener != null) {
            if (subscriptionId == null) {
                getSubscriptionIdListeners.add(listener);
            } else {
                listener.subscribed(subscriptionId);
            }
        }
    }

    public synchronized void setSubscriptionId(String value) {
        subscriptionId = value;
        notifyAll();

        if (subscriptionId != null) {
            for (SubscribedListener listener : getSubscriptionIdListeners) {
                listener.subscribed(subscriptionId);
            }
            getSubscriptionIdListeners = new ArrayList<>();
        }
    }

    public boolean isNotificationReceivedListenerCallback() {
        return notificationReceivedListener != null && notificationReceivedListener instanceof NotificationReceivedCallbackListener;
    }

    public boolean fireNotificationReceivedListener(final NotificationOpenedResult openedResult) {
        if (notificationReceivedListener == null) {
            return false;
        }
        notificationReceivedListener.notificationReceived(openedResult);
        return true;
    }

    public boolean fireNotificationReceivedCallbackListener(final NotificationOpenedResult openedResult) {
        if (notificationReceivedListener == null || !(notificationReceivedListener instanceof NotificationReceivedCallbackListener)) {
            return false;
        }
        return ((NotificationReceivedCallbackListener) notificationReceivedListener).notificationReceivedCallback(openedResult);
    }

    public void removeNotificationReceivedListener() {
        notificationReceivedListener = null;
    }

    public boolean fireNotificationOpenedListener(final NotificationOpenedResult openedResult) {
		if (openedResult.getNotification().getAppBanner() != null) {
			showAppBanner(openedResult.getNotification().getAppBanner(), openedResult.getNotification().getId());
		}

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
        if (subscribedListener == null ||  subscriptionId == null) {
            return;
        }
        subscribedListener.subscribed(subscriptionId);
    }

    public void removeSubscribedListener() {
        subscribedListener = null;
    }

    private SubscriptionManager subscriptionManager;

    public SubscriptionManager getSubscriptionManager() {
        if (subscriptionManager != null) {
            return subscriptionManager;
        }
        if (supportsADM()) {
            subscriptionManager = new SubscriptionManagerADM(CleverPush.context);
        } else if ((hasFCMLibrary() || hasGCMLibrary()) && isGMSInstalledAndEnabled()) {
            subscriptionManager = new SubscriptionManagerFCM(CleverPush.context);
        } else if (supportsHMS()) {
            subscriptionManager = new SubscriptionManagerHMS(CleverPush.context);
        } else {
            subscriptionManager = new SubscriptionManagerFCM(CleverPush.context);
        }

        return subscriptionManager;
    }

    public void setSubscriptionManager(SubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    static boolean hasFCMLibrary() {
        try {
            Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasGCMLibrary() {
        try {
            Class.forName("com.google.android.gms.gcm.GoogleCloudMessaging");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean isGMSInstalledAndEnabled() {
        try {
            PackageManager pm = CleverPush.context.getPackageManager();
            PackageInfo info = pm.getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, PackageManager.GET_META_DATA);
            return info.applicationInfo.enabled;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private static boolean hasHMSAvailabilityLibrary() {
        try {
            Class.forName("com.huawei.hms.api.HuaweiApiAvailability");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasHMSPushKitLibrary() {
        try {
            Class.forName("com.huawei.hms.aaid.HmsInstanceId");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static boolean hasHMSAGConnectLibrary() {
        try {
            Class.forName("com.huawei.agconnect.config.AGConnectServicesConfig");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    static boolean hasAllHMSLibrariesForPushKit() {
        return hasHMSAGConnectLibrary() && hasHMSPushKitLibrary();
    }

    private static boolean isHMSCoreInstalledAndEnabled() {
        HuaweiApiAvailability availability = HuaweiApiAvailability.getInstance();
        return availability.isHuaweiMobileServicesAvailable(CleverPush.context) == 0;
    }

    private boolean supportsADM() {
        try {
            Class.forName("com.amazon.device.messaging.ADM");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private boolean supportsHMS() {
        if (!hasHMSAvailabilityLibrary() || !hasAllHMSLibrariesForPushKit()) {
            return false;
        }
        return isHMSCoreInstalledAndEnabled();
    }

    public Set<String> getSubscriptionTags() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
    }

    public Set<String> getSubscriptionTopics() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>());
    }

    public boolean hasSubscriptionTopics() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_TOPICS);
    }

    public boolean hasDeSelectAll() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getBoolean(CleverPushPreferences.SUBSCRIPTION_TOPICS_DESELECT_ALL, false);
    }

    public void setDeSelectAll(Boolean isDeSelectAll) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(CleverPushPreferences.SUBSCRIPTION_TOPICS_DESELECT_ALL, isDeSelectAll);
        editor.commit();
    }

    public Map<String, String> getSubscriptionAttributes() {
        Map<String, String> outputMap = new HashMap<>();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        try {
            if (sharedPreferences != null) {
                String jsonString = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString());
                JSONObject jsonObject = new JSONObject(jsonString);
                Iterator<String> keysItr = jsonObject.keys();
                while (keysItr.hasNext()) {
                    String k = keysItr.next();
                    String v = (String) jsonObject.get(k);
                    outputMap.put(k, v);
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
            Gson gson = new Gson();
            try {
                JSONArray tagsArray = channelConfig.getJSONArray("channelTags");
                if (tagsArray != null) {
                    for (int i = 0; i < tagsArray.length(); i++) {
                        JSONObject tagObject = tagsArray.getJSONObject(i);
                        if (tagObject != null) {
                            ChannelTag tag = gson.fromJson(tagObject.toString(), ChannelTag.class);
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


    @Deprecated
    public Set<ChannelTag> getAvailableTags() {
        JSONObject channelConfig = this.getChannelConfig();
        return this.getAvailableTagsFromConfig(channelConfig);
    }

    public void getAvailableTags(ChannelTagsListener listener) {
        this.getChannelConfig(channelConfig -> {
            listener.ready(this.getAvailableTagsFromConfig(channelConfig));
        });
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
        this.getChannelConfig(channelConfig -> {
            listener.ready(this.getAvailableAttributesFromConfig(channelConfig));
        });
    }

    public void getAvailableTopics(ChannelTopicsListener listener) {
        this.getChannelConfig(channelConfig -> {
            listener.ready(this.getAvailableTopicsFromConfig(channelConfig));
        });
    }

    private Set<ChannelTopic> getAvailableTopicsFromConfig(JSONObject channelConfig) {
        Set<ChannelTopic> topics = new LinkedHashSet<>();
        if (channelConfig != null && channelConfig.has("channelTopics")) {
            try {
                JSONArray topicsArray = channelConfig.getJSONArray("channelTopics");
                if (topicsArray != null) {
                    for (int i = 0; i < topicsArray.length(); i++) {
                        JSONObject topicObject = topicsArray.getJSONObject(i);
                        if (topicObject != null) {
                            Map<String, String> customData = null;
                            try {
                                JSONObject customDataObject = topicObject.optJSONObject("customData");
                                if (customDataObject != null) {
                                    customData = new Gson().fromJson(customDataObject.toString(), HashMap.class);
                                }
                            } catch (Exception ignored) {
                            }

                            ChannelTopic topic = new ChannelTopic(
                                    topicObject.getString("_id"),
                                    topicObject.optString("name"),
                                    topicObject.optString("parentTopic", null),
                                    topicObject.optBoolean("defaultUnchecked", false),
                                    topicObject.optString("fcmBroadcastTopic", null),
                                    topicObject.optString("externalId", null),
                                    customData
                            );
                            topics.add(topic);
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.d("CleverPush", ex.getMessage(), ex);
            }
        }
        return topics;
    }

    public void addSubscriptionTag(String tagId) {
        Log.d("CleverPush", "addSubscriptionTag: " + tagId);
        this.waitForTrackingConsent(() -> new Thread(() -> this.getSubscriptionId(subscriptionId -> {
            if (subscriptionId != null) {
                Set<String> tags = this.getSubscriptionTags();
                if (tags.contains(tagId)) {
                    Log.d("CleverPush", "Subscription already has tag - skipping API call " + tagId);
                    return;
                }

                JSONObject jsonBody = new JSONObject();
                try {
                    jsonBody.put("channelId", this.channelId);
                    jsonBody.put("tagId", tagId);
                    jsonBody.put("subscriptionId", subscriptionId);
                } catch (JSONException ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }

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
        })).start());
    }

    public void removeSubscriptionTag(String tagId) {
        this.waitForTrackingConsent(() -> new Thread(() -> this.getSubscriptionId(subscriptionId -> {
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
        })).start());
    }

    public void setSubscriptionTopics(String[] topicIds) {
        setSubscriptionTopics(topicIds, null);
    }

    public void setSubscriptionTopics(String[] topicIds, CompletionListener completionListener) {
        new Thread(() -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

            final int topicsVersion = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, 0) + 1;

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CleverPushPreferences.SUBSCRIPTION_TOPICS).apply();
            editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>(Arrays.asList(topicIds)));
            editor.putInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, topicsVersion);
            editor.commit();


            this.getSubscriptionId(subscriptionId -> {
                if (subscriptionId != null) {
                    JSONObject jsonBody = new JSONObject();
                    try {
                        JSONArray topicsArray = new JSONArray();
                        for (String topicId : topicIds) {
                            topicsArray.put(topicId);
                        }

                        jsonBody.put("channelId", this.channelId);
                        jsonBody.put("topics", topicsArray);
                        jsonBody.put("topicsVersion", topicsVersion);
                        jsonBody.put("subscriptionId", subscriptionId);
                    } catch (JSONException ex) {
                        Log.e("CleverPush", ex.getMessage(), ex);
                    }

                    Log.d("CleverPush", "setSubscriptionTopics: " + Arrays.toString(topicIds));


                    CleverPush instance = this;
                    CleverPushHttpClient.post("/subscription/sync/" + this.channelId, jsonBody, new CleverPushHttpClient.ResponseHandler() {
                        @Override
                        public void onSuccess(String response) {
                            TopicsChangedListener topicsChangedListener = instance.getTopicsChangedListener();
                            if (topicsChangedListener != null) {
                                topicsChangedListener.changed(new HashSet<>(Arrays.asList(topicIds)));
                            }
                            if (completionListener != null) {
                                completionListener.onComplete();
                            }
                        }

                        @Override
                        public void onFailure(int statusCode, String response, Throwable throwable) {
                            Log.e("CleverPush", "Error setting topics - HTTP " + statusCode + ": " + response);
                        }
                    });
                }
            });
        }).start();
    }

    public void setSubscriptionAttribute(String attributeId, String value) {
        this.waitForTrackingConsent(() -> new Thread(() -> this.getSubscriptionId(subscriptionId -> {
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
        })).start());
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
        if (encodedNotifications != null) {
            Gson gson = new Gson();
            for (String encodedNotification : encodedNotifications) {
                Notification notification = gson.fromJson(encodedNotification, Notification.class);
                notifications.add(notification);
            }
        }
        return notifications;
    }

    public void trackEvent(String eventName) {
        this.trackEvent(eventName, null);
    }

    public void trackEvent(String eventName, Float amount) {
        this.getChannelConfig(channelConfig -> {
            if (channelConfig == null) {
                return;
            }

            try {
                JSONArray channelEvents = channelConfig.getJSONArray("channelEvents");
                JSONObject event = null;
                for (int i = 0; i < channelEvents.length(); i++) {
                    JSONObject tryEvent = channelEvents.getJSONObject(i);
                    if (tryEvent != null && tryEvent.getString("name").equals(eventName)) {
                        event = tryEvent;
                        break;
                    }
                }

                if (event == null) {
                    Log.e("CleverPush", "Event not found");
                    return;
                }

                this.getSubscriptionId(subscriptionId -> {
                    if (subscriptionId != null) {
                        JSONObject jsonBody = new JSONObject();
                        try {
                            jsonBody.put("channelId", this.channelId);
                            jsonBody.put("eventName", eventName);
                            jsonBody.put("amount", amount);
                            jsonBody.put("subscriptionId", subscriptionId);
                        } catch (JSONException ex) {
                            Log.e("CleverPush", ex.getMessage(), ex);
                        }

                        CleverPushHttpClient.post("/subscription/conversion", jsonBody, new CleverPushHttpClient.ResponseHandler() {
                            @Override
                            public void onSuccess(String response) {
                                Log.d("CleverPush", "Event successfully tracked: " + eventName);
                            }

                            @Override
                            public void onFailure(int statusCode, String response, Throwable throwable) {
                                Log.e("CleverPush", "Error tracking event - HTTP " + statusCode);
                            }
                        });
                    }
                });

            } catch (Exception ex) {
                Log.e("CleverPush", ex.getMessage());
            }
        });
    }

	public void trackNotificationDelivered(String notificationId) {
		this.getSubscriptionId(subscriptionId -> this.trackNotificationDelivered(notificationId, subscriptionId));
	}

	public void trackNotificationDelivered(String notificationId, String subscriptionId) {
		JSONObject jsonBody = new JSONObject();
		try {
			jsonBody.put("notificationId", notificationId);
			jsonBody.put("subscriptionId", subscriptionId);
		} catch (JSONException e) {
			Log.e("CleverPush", "Error generating delivered json", e);
		}

		CleverPushHttpClient.post("/notification/delivered", jsonBody, null);
	}

	public void trackNotificationClicked(String notificationId) {
    	this.getSubscriptionId(subscriptionId -> this.trackNotificationClicked(notificationId, subscriptionId));
	}

    public void trackNotificationClicked(String notificationId, String subscriptionId) {
		JSONObject jsonBody = new JSONObject();
		try {
			jsonBody.put("notificationId", notificationId);
			jsonBody.put("subscriptionId", subscriptionId);
		} catch (JSONException e) {
			Log.e("CleverPush", "Error generating clicked json", e);
		}

		CleverPushHttpClient.post("/notification/clicked", jsonBody, null);
	}

	public void triggerAppBannerEvent(String key, String value) {
    	if (this.appBannerModule == null) {
			pendingAppBannerEvents.put(key, value);
    		return;
		}
		this.appBannerModule.triggerEvent(key, value);
	}

    public void showAppBanner(String bannerId) {
		showAppBanner(bannerId, null);
    }

	public void showAppBanner(String bannerId, String notificationId) {
		if (appBannerModule == null) {
			pendingShowAppBannerId = bannerId;
			pendingShowAppBannerNotificationId = notificationId;
			return;
		}
		appBannerModule.showBannerById(bannerId, notificationId);
	}

    private void showPendingTopicsDialog() {
        this.getChannelConfig(config -> {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
            try {
                if (config != null && sharedPreferences.getBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, false)) {
                    final int topicsDialogSeconds = config.optInt("topicsDialogMinimumSeconds", 0);
                    int topicsDialogSessions = config.optInt("topicsDialogMinimumSessions", 0);
                    int topicsDialogDays = config.optInt("topicsDialogMinimumDays", 0);

                    long currentUnixTime = System.currentTimeMillis() / 1000L;
                    long allowedUnixTime = sharedPreferences.getLong(CleverPushPreferences.SUBSCRIPTION_CREATED_AT, 0) + (topicsDialogDays * 60 * 60 * 24);
                    int appOpens = sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 1);

                    if (currentUnixTime >= allowedUnixTime && appOpens >= topicsDialogSessions) {
                        (ActivityLifecycleListener.currentActivity).runOnUiThread(() -> {
                            new Handler().postDelayed(() -> {
                                if (sharedPreferences.getBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, false)) {
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, false);
                                    editor.commit();

                                    this.showTopicsDialog();
                                }
                            }, topicsDialogSeconds * 1000);
                        });
                    }
                }
            } catch (Exception ex) {
                Log.d("CleverPush", ex.getMessage());
            }
        });
    }

    public void showTopicsDialog() {
        showTopicsDialog(ActivityLifecycleListener.currentActivity);
    }

    public void showTopicsDialog(Context dialogActivity) {
        showTopicsDialog(dialogActivity, null);
    }

    public void showTopicsDialog(Context dialogActivity, TopicsDialogListener topicsDialogListener) {
        try {
            if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M)
                showTopicsDialog(dialogActivity, topicsDialogListener, R.style.Theme_AppCompat_Dialog_Alert);
            else
                showTopicsDialog(dialogActivity, topicsDialogListener, 0);
        } catch (IllegalStateException ex) {
            showTopicsDialog(dialogActivity, topicsDialogListener, R.style.Theme_AppCompat_Dialog_Alert);
        }
    }

    public void showTopicsDialog(Context dialogActivity, TopicsDialogListener topicsDialogListener, @StyleRes int themeResId) {
    	// Ensure it will only be shown once at a time
    	if (showingTopicsDialog) {
    		return;
		}

        int nightModeFlags = CleverPush.context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        this.getChannelConfig(channelConfig -> {
            if (channelConfig == null) {
                if (topicsDialogListener != null) {
                    topicsDialogListener.callback(false);
                }
				showingTopicsDialog = false;
                return;
            }

            try {
                JSONArray channelTopics = channelConfig.getJSONArray("channelTopics");
                if (channelTopics.length() == 0) {
                    Log.w("CleverPush", "CleverPush: showTopicsDialog: No topics found. Create some first in the CleverPush channel settings.");
                }

                ((Activity) dialogActivity).runOnUiThread(() -> {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(dialogActivity, themeResId);

                    String headerTitle = CleverPush.context.getResources().getString(R.string.topics_dialog_title);
                    if (channelConfig.has("confirmAlertSelectTopicsLaterTitle")) {
                        try {
                            headerTitle = channelConfig.getString("confirmAlertSelectTopicsLaterTitle");
                        } catch (Exception ignored) {}
                    }

                    alertBuilder.setTitle(headerTitle);

                    final boolean[] checkedTopics = new boolean[channelTopics.length()];
                    String[] topicIds = new String[channelTopics.length()];

                    LinearLayout checkboxLayout = new LinearLayout(context);
                    checkboxLayout.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                    checkboxLayout.setOrientation(LinearLayout.VERTICAL);

                    LinearLayout parentLayout = new LinearLayout(context);
                    LinearLayout.LayoutParams parentLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    parentLayoutParams.setMargins(45, 45, 45, 45);
                    parentLayout.setLayoutParams(parentLayoutParams);
                    parentLayout.setOrientation(LinearLayout.VERTICAL);

                    CheckBox checkboxDeSelectAll = new CheckBox(CleverPush.context);
                    checkboxDeSelectAll.setText(context.getText(R.string.deselect_all));
                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                        checkboxDeSelectAll.setTextColor(Color.WHITE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            checkboxDeSelectAll.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
                        }
                    }

                    if (hasDeSelectAll()) {
                        setCheckboxList(parentLayout, checkboxDeSelectAll, channelTopics, checkedTopics, topicIds, true, nightModeFlags);
                    } else {
                        setCheckboxList(parentLayout, checkboxDeSelectAll, channelTopics, checkedTopics, topicIds, false, nightModeFlags);
                    }

                    checkboxLayout.addView(parentLayout);

                    alertBuilder.setView(checkboxLayout);

                    alertBuilder.setNegativeButton(CleverPush.context.getResources().getString(R.string.cancel), (dialogInterface, i) -> {
                        if (topicsDialogListener != null) {
                            topicsDialogListener.callback(false);
                        }
						showingTopicsDialog = false;
                    });
                    alertBuilder.setPositiveButton(CleverPush.context.getResources().getString(R.string.save), (dialogInterface, i) -> {
                        if (checkboxDeSelectAll.isChecked()) {
                            unsubscribe();
                            setDeSelectAll(true);
                        } else {
                            setDeSelectAll(false);
                            Set<String> selectedTopicIds = new HashSet<>();
                            for (int j = 0; j < topicIds.length; j++) {
                                if (checkedTopics[j]) {
                                    selectedTopicIds.add(topicIds[j]);
                                }
                            }
                            CleverPush.getInstance(CleverPush.context).setSubscriptionTopics(selectedTopicIds.toArray(new String[0]));
                        }

                        dialogInterface.dismiss();

                        if (topicsDialogListener != null) {
                            topicsDialogListener.callback(true);
                        }
						showingTopicsDialog = false;
                    });

                    AlertDialog alert = alertBuilder.create();
                    alert.setOnShowListener(dialog -> {
						Log.d("CleverPush", "showTopicsDialog activity: " + dialogActivity.getClass().getCanonicalName());
						showingTopicsDialog = true;
					});
					alert.show();
                });

            } catch (JSONException e) {
				showingTopicsDialog = false;
                Log.e("CleverPush", "Error getting channel topics " + e.getMessage());
            }
        });
    }

    /**
     * Will create list of checkbox for the topics.
     * @param parentLayout        parent layout to add checkboxes
     * @param checkboxDeSelectAll checkBox to deselect all the topis
     * @param channelTopics       topics from the channel
     * @param checkedTopics       userSelectedTopics
     * @param isDeselectAll       is deselectall checkbox is checked or not
     * @param nightModeFlags      flag if there is night mode
     */
    private void setCheckboxList(LinearLayout parentLayout, CheckBox checkboxDeSelectAll, JSONArray channelTopics, boolean[] checkedTopics, String[] topicIds, boolean isDeselectAll, int nightModeFlags) {
        try {
            parentLayout.removeAllViews();
            Set<String> selectedTopics = instance.getSubscriptionTopics();

            if (channelConfig.optBoolean("topicsDialogShowUnsubscribe",false)) {
                checkboxDeSelectAll.setChecked(isDeselectAll);
                parentLayout.addView(checkboxDeSelectAll);
                checkboxDeSelectAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        for (int i = 0; i < channelTopics.length(); i++) {
                            try {
                                JSONObject channelTopic = (JSONObject) channelTopics.get(i);
                                channelTopic.put("defaultUnchecked", true);
                                selectedTopics.clear();
                                checkedTopics[i] = false;
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        setCheckboxList(parentLayout, checkboxDeSelectAll, channelTopics, checkedTopics, topicIds, true, nightModeFlags);
                    }
                });
            }

            for (int i = 0; i < channelTopics.length(); i++) {
                final int parentIndex = i;
                JSONObject topic = channelTopics.getJSONObject(i);
                if (topic != null) {
                    boolean defaultUnchecked = false;
                    try {
                        defaultUnchecked = topic.optBoolean("defaultUnchecked");
                    } catch (Exception ignored) {
                    }
                    String id = topic.getString("_id");
                    topicIds[i] = id;

                    CheckBox checkbox = new CheckBox(CleverPush.context);
                    checkbox.setText(topic.optString("name"));

                    if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                        checkbox.setTextColor(Color.WHITE);
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            checkbox.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
                        }
                    }

                    if (!hasDeSelectAll()) {
                        checkbox.setChecked((selectedTopics.size() == 0 && !this.hasSubscriptionTopics() && !defaultUnchecked) || selectedTopics.contains(id));
                    }

                    if (topic.has("parentTopic") && topic.optString("parentTopic").length() > 0) {
                        continue;
                    }

                    parentLayout.addView(checkbox);

                    int addedChildren = 0;
                    final LinearLayout childLayout = new LinearLayout(context);
                    for (int j = 0; j < channelTopics.length(); j++) {
                        final int childIndex = j;
                        JSONObject childTopic = channelTopics.getJSONObject(j);
                        if (childTopic != null && childTopic.optString("parentTopic").equals(id)) {
                            String childId = childTopic.getString("_id");
                            boolean childDefaultUnchecked = false;
                            try {
                                childDefaultUnchecked = topic.optBoolean("defaultUnchecked");
                            } catch (Exception ignored) {
                            }

                            addedChildren++;

                            LinearLayout.LayoutParams childLayoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            childLayoutParams.setMargins(65, 0, 0, 20);
                            childLayout.setLayoutParams(childLayoutParams);
                            childLayout.setOrientation(LinearLayout.VERTICAL);

                            CheckBox checkboxChild = new CheckBox(CleverPush.context);
                            checkboxChild.setOnCheckedChangeListener((buttonView, isChecked) -> {
                                checkedTopics[childIndex] = isChecked;
                                checkboxDeSelectAll.setChecked(false);
                            });
                            checkedTopics[childIndex] = checkbox.isChecked();

                            checkboxChild.setText(childTopic.optString("name"));

                            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                                checkboxChild.setTextColor(Color.WHITE);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                    checkboxChild.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
                                }
                            }

                            if (!hasDeSelectAll()) {
                                checkboxChild.setChecked((selectedTopics.size() == 0 && !this.hasSubscriptionTopics() && !childDefaultUnchecked) || selectedTopics.contains(childId));
                            }

                            childLayout.setVisibility(checkbox.isChecked() ? View.VISIBLE : View.GONE);

                            childLayout.addView(checkboxChild);
                        }
                    }

                    checkedTopics[parentIndex] = checkbox.isChecked();
                    final boolean hasChildren = addedChildren > 0;
                    checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        checkboxDeSelectAll.setChecked(false);
                        checkedTopics[parentIndex] = isChecked;
                        if (hasChildren) {
                            childLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                        }
                    });

                    if (addedChildren > 0) {
                        parentLayout.addView(childLayout);
                    }

                } else {
                    Log.e("CleverPush", "topic is null");
                }
            }
        } catch (JSONException e) {
            Log.e("CleverPush", "Error getting channel topics " + e.getMessage());
        }

    }

    public void setApiEndpoint(String apiEndpoint) {
        CleverPushHttpClient.BASE_URL = apiEndpoint;
    }

    static void setAppContext(Context newAppContext) {
        context = newAppContext.getApplicationContext();
    }

    public void setBrandingColor(int color) {
        brandingColor = color;
    }

    public int getBrandingColor() {
        return brandingColor;
    }

    public void setChatUrlOpenedListener(ChatUrlOpenedListener listener) {
        chatUrlOpenedListener = listener;
    }

    public ChatUrlOpenedListener getChatUrlOpenedListener() {
        return chatUrlOpenedListener;
    }

    public void setChatSubscribeListener(ChatSubscribeListener listener) {
        chatSubscribeListener = listener;
    }

    public ChatSubscribeListener getChatSubscribeListener() {
        return chatSubscribeListener;
    }

    public void setTopicsChangedListener(TopicsChangedListener listener) {
        topicsChangedListener = listener;
    }

    public void setAppBannerOpenedListener(AppBannerOpenedListener listener) {
        appBannerOpenedListener = listener;
    }

    public AppBannerOpenedListener getAppBannerOpenedListener() {
        return appBannerOpenedListener;
    }

    public TopicsChangedListener getTopicsChangedListener() {
        return topicsChangedListener;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == locationPermissionRequestCode && (geofenceList == null || geofenceList.size() == 0)) {
            this.initGeoFences();
        }
    }

    public void setAutoClearBadge(boolean autoClearBadge) {
        this.autoClearBadge = autoClearBadge;
    }

    public boolean getAutoClearBadge() {
        return autoClearBadge;
    }

    public void setIncrementBadge(boolean incrementBadge) {
        this.incrementBadge = incrementBadge;
    }

    public boolean getIncrementBadge() {
        return incrementBadge;
    }

    public void enableDevelopmentMode() {
        Log.w("CleverPush", "CleverPush SDK is running in development mode. Only use this for testing!");
        this.developmentMode = true;
    }

    public boolean isDevelopmentModeEnabled() {
        return this.developmentMode;
    }

    private void clearSubscriptionData() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_ID).apply();
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC).apply();
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_CREATED_AT).apply();
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_TOPICS).apply();
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION).apply();
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_TAGS).apply();
        sharedPreferences.edit().remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES).apply();
    }

	public boolean areAppBannersDisabled() {
    	return appBannersDisabled;
	}

    public void enableAppBanners() {
		appBannersDisabled = false;
    	if (appBannerModule == null) {
			return;
		}
		appBannerModule.enableBanners();
    }

    public void disableAppBanners() {
		appBannersDisabled = true;
		if (appBannerModule == null) {
			return;
		}
		appBannerModule.disableBanners();
    }
}
