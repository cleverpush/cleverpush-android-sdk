package com.cleverpush;

import static com.cleverpush.Constants.IABTCF_VendorConsent_POSITION;
import static com.cleverpush.Constants.IABTCF_VendorConsents;
import static com.cleverpush.Constants.LOG_TAG;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
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
import android.view.View;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;

import com.cleverpush.banner.AppBannerModule;
import com.cleverpush.database.DatabaseClient;
import com.cleverpush.database.TableBannerTrackEvent;
import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.listener.AppBannerShownListener;
import com.cleverpush.listener.AppBannersListener;
import com.cleverpush.listener.ChannelAttributesListener;
import com.cleverpush.listener.ChannelConfigListener;
import com.cleverpush.listener.ChannelTagsListener;
import com.cleverpush.listener.ChannelTopicsListener;
import com.cleverpush.listener.ChatSubscribeListener;
import com.cleverpush.listener.ChatUrlOpenedListener;
import com.cleverpush.listener.CompletionFailureListener;
import com.cleverpush.listener.CompletionListener;
import com.cleverpush.listener.DeviceTokenListener;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.LogListener;
import com.cleverpush.listener.NotificationOpenedCallbackListener;
import com.cleverpush.listener.NotificationOpenedListenerBase;
import com.cleverpush.listener.NotificationReceivedCallbackListener;
import com.cleverpush.listener.NotificationReceivedListenerBase;
import com.cleverpush.listener.NotificationsCallbackListener;
import com.cleverpush.listener.SessionListener;
import com.cleverpush.listener.StopCampaignListener;
import com.cleverpush.listener.SubscribeConsentListener;
import com.cleverpush.listener.SubscribedCallbackListener;
import com.cleverpush.listener.SubscribedListener;
import com.cleverpush.listener.TopicsChangedListener;
import com.cleverpush.listener.TopicsDialogListener;
import com.cleverpush.listener.TrackingConsentListener;
import com.cleverpush.listener.UnsubscribedListener;
import com.cleverpush.listener.WebViewClientListener;
import com.cleverpush.manager.SubscriptionManager;
import com.cleverpush.manager.SubscriptionManagerADM;
import com.cleverpush.manager.SubscriptionManagerFCM;
import com.cleverpush.manager.SubscriptionManagerHMS;
import com.cleverpush.mapper.Mapper;
import com.cleverpush.mapper.SubscriptionToListMapper;
import com.cleverpush.responsehandlers.ChannelConfigFromBundleIdResponseHandler;
import com.cleverpush.responsehandlers.ChannelConfigFromChannelIdResponseHandler;
import com.cleverpush.responsehandlers.SetSubscriptionAttributeResponseHandler;
import com.cleverpush.responsehandlers.SetSubscriptionTopicsResponseHandler;
import com.cleverpush.responsehandlers.StopCampaignResponseHandler;
import com.cleverpush.responsehandlers.TrackEventResponseHandler;
import com.cleverpush.responsehandlers.TrackSessionStartResponseHandler;
import com.cleverpush.responsehandlers.TriggerFollowUpEventResponseHandler;
import com.cleverpush.responsehandlers.UnsubscribeResponseHandler;
import com.cleverpush.service.NotificationDataProcessor;
import com.cleverpush.service.StoredNotificationsCursor;
import com.cleverpush.service.StoredNotificationsService;
import com.cleverpush.service.TagsMatcher;
import com.cleverpush.util.BroadcastReceiverUtils;
import com.cleverpush.util.Logger;
import com.cleverpush.util.MetaDataUtils;
import com.cleverpush.util.NotificationCategorySetUp;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.huawei.hms.api.HuaweiApiAvailability;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class CleverPush {

  public static final String SDK_VERSION = "1.33.13";

  private static CleverPush instance;
  private static boolean isSubscribeForTopicsDialog = false;

  public static Context context;

  private NotificationReceivedListenerBase notificationReceivedListener;
  private NotificationOpenedListenerBase notificationOpenedListener;
  private SubscribedListener subscribedListener;
  private ChatUrlOpenedListener chatUrlOpenedListener;
  private ChatSubscribeListener chatSubscribeListener;
  private TopicsChangedListener topicsChangedListener;
  private AppBannerShownListener appBannerShownListener;
  private AppBannerOpenedListener appBannerOpenedListener;
  private Collection<SubscribedListener> getSubscriptionIdListeners = new ArrayList<>();
  private static Collection<ChannelConfigListener> getChannelConfigListeners = new ArrayList<>();
  private final Collection<NotificationOpenedResult> unprocessedOpenedNotifications = new ArrayList<>();
  private SessionListener sessionListener;
  private WebViewClientListener webViewClientListener;
  private GoogleApiClient googleApiClient;
  private final ArrayList<Geofence> geofenceList = new ArrayList<>();
  private final Map<String, Boolean> autoAssignSessionsCounted = new HashMap<>();
  private List<TriggeredEvent> pendingAppBannerEvents = new ArrayList<>();
  private String pendingShowAppBannerId = null;
  private String pendingShowAppBannerNotificationId = null;
  private String currentPageUrl;
  private AppBannerModule appBannerModule;
  private boolean appBannersDisabled = false;
  private boolean isAppOpen = false;
  private Boolean pendingAppBannerTrackingEnabled = null;
  private long notificationOpenedActivityDestroyedAt = 0;

  private String channelId;
  private String subscriptionId = null;
  private static JSONObject channelConfig = null;
  private boolean subscriptionInProgress = false;
  private static boolean initialized = false;
  private int brandingColor;
  private boolean pendingRequestLocationPermissionCall = false;
  private boolean pendingInitFeaturesCall = false;
  private ArrayList<PageView> pendingPageViews = new ArrayList<>();

  private int sessionVisits = 0;
  private long sessionStartedTimestamp = 0;

  private boolean trackingConsentRequired = false;
  private boolean hasTrackingConsent = false;
  private boolean hasTrackingConsentCalled = false;
  private Collection<TrackingConsentListener> trackingConsentListeners = new ArrayList<>();

  private boolean subscribeConsentRequired = false;
  private boolean hasSubscribeConsent = false;
  private boolean hasSubscribeConsentCalled = false;
  private Collection<SubscribeConsentListener> subscribeConsentListeners = new ArrayList<>();

  private boolean incrementBadge = false;
  private boolean autoClearBadge = false;
  public boolean isShowDraft = false;
  private boolean ignoreDisabledNotificationPermission = false;
  private boolean keepTargetingDataOnUnsubscribe = false;

  private boolean developmentMode = false;

  private boolean disableNightModeAdaption = false;

  private boolean showingTopicsDialog = false;
  private boolean confirmAlertShown = false;
  private boolean topicsDialogShowWhenNewAdded = false;
  private InitializeListener initializeListener;

  private AddSubscriptionTags addSubscriptionTagsHelper;
  private RemoveSubscriptionTags removeSubscriptionTagsHelper;

  private Activity customActivity = null;

  private final String DATE_FORMAT_ISO = "yyyy-MM-dd HH:mm:ss z";
  private final int SYNC_SUBSCRIPTION_INTERVAL = 3 * 60 * 60 * 24;
  private final long SECONDS_PER_DAY = 60 * 60 * 24;
  private final long MILLISECONDS_PER_SECOND = 1000;

  private boolean pendingRequestNotificationPermissionCall = false;
  private SubscribedCallbackListener pendingSubscribeCallbackListener = null;

  public static BroadcastReceiver broadcastReceiverHandler = new BroadcastReceiverHandler();
  private PendingIntent geofencePendingIntent;
  private GeofencingClient geofencingClient;

  private String lastClickedNotificationId;
  private long lastClickedNotificationTime;
  private String authorizerToken;
  private boolean isSubscriptionChanged = false;
  private IabTcfMode iabTcfMode = null;
  private int trackEventRetentionDays = 90;
  private boolean autoResubscribe = false;
  private boolean autoRequestNotificationPermission = true;
  private boolean isSessionStartCalled = false;

  public CleverPush(@NonNull Context context) {
    if (context == null) {
      return;
    }

    if (context instanceof Application) {
      CleverPush.context = context;
    } else {
      CleverPush.context = context.getApplicationContext();
    }

    sessionListener = initSessionListener();

    if ((Application) getContext() != null) {
      if (context instanceof Activity) {
        ActivityLifecycleListener.registerActivityLifecycleCallbacks((Application) getContext(), sessionListener,
            (Activity) context);
      } else {
        ActivityLifecycleListener.registerActivityLifecycleCallbacks((Application) getContext(), sessionListener);
      }
    }

    geofencingClient = LocationServices.getGeofencingClient(context);
  }

  /**
   * initialize CleverPush SDK
   */
  public void init() {
    init(null, null, null, null, true);
  }

  /**
   * initialize CleverPush SDK with notification received callback
   *
   * @param notificationReceivedListener callback for the notification received
   */
  public void init(@Nullable final NotificationReceivedListenerBase notificationReceivedListener) {
    String channelId = MetaDataUtils.getChannelId(CleverPush.context);
    init(channelId, notificationReceivedListener);
  }

  /**
   * initialize CleverPush SDK with notification opened callback
   *
   * @param notificationOpenedListener callback for the notification opened
   */
  public void init(@Nullable final NotificationOpenedListenerBase notificationOpenedListener) {
    String channelId = MetaDataUtils.getChannelId(CleverPush.context);
    init(channelId, notificationOpenedListener);
  }

  /**
   * initialize CleverPush SDK with subscribed callback
   *
   * @param subscribedListener callback for subscription
   */

  public void init(@Nullable final SubscribedListener subscribedListener) {
    String channelId = MetaDataUtils.getChannelId(CleverPush.context);
    init(channelId, subscribedListener);
  }

  /**
   * initialize CleverPush SDK for channel
   *
   * @param channelId channelID of the channel
   */
  public void init(String channelId) {
    init(channelId, null, null, null);
  }

  /**
   * initialize CleverPush SDK for channel with notification received callback
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener) {
    init(channelId, notificationReceivedListener, null, null);
  }

  /**
   * initialize CleverPush SDK for channel with notification opened callback
   *
   * @param channelId                  channelID of the channel
   * @param notificationOpenedListener callback for the notification opened
   */
  public void init(String channelId, @Nullable final NotificationOpenedListenerBase notificationOpenedListener) {
    init(channelId, null, notificationOpenedListener, null);
  }

  /**
   * initialize CleverPush SDK with notification opened callback and subscribed callback
   *
   * @param notificationOpenedListener callback for the notification opened
   * @param subscribedListener         callback for subscription
   */
  public void init(@Nullable final NotificationOpenedListenerBase notificationOpenedListener,
                   @Nullable final SubscribedListener subscribedListener) {
    init(null, null, notificationOpenedListener, subscribedListener);
  }

  /**
   * initialize CleverPush SDK with notification received callback and subscribed callback
   *
   * @param notificationReceivedListener callback for the notification received
   * @param subscribedListener           callback for subscription
   */
  public void init(@Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final SubscribedListener subscribedListener) {
    init(null, notificationReceivedListener, null, subscribedListener);
  }

  /**
   * initialize CleverPush SDK for channel with notification received callback and notification opened callback
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   * @param notificationOpenedListener   callback for the notification opened
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final NotificationOpenedListenerBase notificationOpenedListener) {
    init(channelId, notificationReceivedListener, notificationOpenedListener, null);
  }

  /**
   * initialize CleverPush SDK for channel with subscribed callback
   *
   * @param channelId          channelID of the channel
   * @param subscribedListener callback for subscription
   */
  public void init(String channelId, @Nullable final SubscribedListener subscribedListener) {
    init(channelId, null, null, subscribedListener);
  }

  /**
   * initialize CleverPush SDK for channel with notification received callback and subscribed callback
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   * @param subscribedListener           callback for subscription
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final SubscribedListener subscribedListener) {
    init(channelId, notificationReceivedListener, null, subscribedListener);
  }

  /**
   * initialize CleverPush SDK for channel with notification opened callback and subscribed callback
   *
   * @param channelId                  channelID of the channel
   * @param notificationOpenedListener callback for the notification opened
   * @param subscribedListener         callback for subscription
   */
  public void init(String channelId, @Nullable final NotificationOpenedListenerBase notificationOpenedListener,
                   @Nullable final SubscribedListener subscribedListener) {
    init(channelId, null, notificationOpenedListener, subscribedListener);
  }

  /**
   * initialize CleverPush SDK for channel with notification received, notification opened callback and subscribed callback
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   * @param notificationOpenedListener   callback for the notification opened
   * @param subscribedListener           callback for subscription
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final NotificationOpenedListenerBase notificationOpenedListener,
                   @Nullable final SubscribedListener subscribedListener) {
    init(channelId, notificationReceivedListener, notificationOpenedListener, subscribedListener, true);
  }

  /**
   * initialize CleverPush SDK for channel with notification opened callback and subscribed callback and if there is autoRegister
   *
   * @param channelId                  channelID of the channel
   * @param notificationOpenedListener callback for the notification opened
   * @param subscribedListener         callback for subscription
   * @param autoRegister               boolean for auto register
   */
  public void init(String channelId, @Nullable final NotificationOpenedListenerBase notificationOpenedListener,
                   @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
    init(channelId, null, notificationOpenedListener, subscribedListener, autoRegister);
  }

  /**
   * initialize CleverPush SDK for channel with notification received callback and subscribed callback and if there is autoRegister
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   * @param subscribedListener           callback for subscription
   * @param autoRegister                 boolean for auto register
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
    init(channelId, notificationReceivedListener, null, subscribedListener, autoRegister);
  }

  /**
   * initialize CleverPush SDK for channel with notification received callback, notification opened and subscribed callback and if there is autoRegister
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   * @param notificationOpenedListener   callback for the notification opened
   * @param subscribedListener           callback for subscription
   * @param autoRegister                 boolean for auto register
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final NotificationOpenedListenerBase notificationOpenedListener,
                   @Nullable final SubscribedListener subscribedListener, boolean autoRegister) {
    init(channelId, notificationReceivedListener, notificationOpenedListener, subscribedListener, autoRegister, null);
  }


  /**
   * initialize CleverPush SDK for channel with notification received callback, notification opened and subscribed callback and if there is autoRegister
   *
   * @param channelId                    channelID of the channel
   * @param notificationReceivedListener callback for the notification received
   * @param notificationOpenedListener   callback for the notification opened
   * @param subscribedListener           callback for subscription
   * @param autoRegister                 boolean for auto register
   * @param initializeListener           callback for the init
   */
  public void init(String channelId, @Nullable final NotificationReceivedListenerBase notificationReceivedListener,
                   @Nullable final NotificationOpenedListenerBase notificationOpenedListener,
                   @Nullable final SubscribedListener subscribedListener, boolean autoRegister,
                   @Nullable final InitializeListener initializeListener) {
    this.channelId = channelId;

    if (notificationReceivedListener != null) {
      this.setNotificationReceivedListener(notificationReceivedListener);
    }
    if (notificationOpenedListener != null) {
      this.setNotificationOpenedListener(notificationOpenedListener);
    }
    if (subscribedListener != null) {
      this.setSubscribedListener(subscribedListener);
    }
    channelConfig = null;
    initialized = false;

    // try to get cached Channel ID from Shared Preferences
    if (this.channelId == null) {
      this.channelId = getChannelId(getContext());
    }

    if (getIabTcfMode() != null && getIabTcfMode() != IabTcfMode.DISABLED) {
      setTCF();
    }

    if (this.channelId != null) {
      Logger.d(LOG_TAG, "Initializing with Channel ID: " + this.channelId + " (SDK " + CleverPush.SDK_VERSION + ")");

      String storedChannelId = getChannelId(getContext());
      String storedSubscriptionId = getSubscriptionId(getContext());

      // Check if the channel id changed. Remove the Subscription ID in this case.
      // Maybe the user switched from Dev to Live environment.
      boolean isUnsubscribing = false;
      if (isChannelIdChanged(storedChannelId, storedSubscriptionId)) {
        try {
          if (subscriptionId != null && !subscriptionId.isEmpty()) {
            isUnsubscribing = true;
            this.unsubscribe(new UnsubscribedListener() {
              @Override
              public void onSuccess() {
                if (initializeListener != null) {
                  initializeListener.onInitialized();
                }
                fireInitializeListener();
              }

              @Override
              public void onFailure(Throwable throwable) {
                if (initializeListener != null) {
                  initializeListener.onInitialized();
                }
                fireInitializeListener();
              }
            });
          } else {
            Logger.d(LOG_TAG, "There is no subscription for CleverPush SDK.");
            this.clearSubscriptionData();
          }
        } catch (Throwable throwable) {
          Logger.e(LOG_TAG, "Error during unsubscribing in init.", throwable);
        }
      }
      addOrUpdateChannelId(getContext(), this.channelId);
      if (!isUnsubscribing) {
        if (initializeListener != null) {
          initializeListener.onInitialized();
        }
        fireInitializeListener();
      }
      // get channel config
      getChannelConfigFromChannelId(autoRegister, storedChannelId, storedSubscriptionId);
    } else {
      Logger.d(LOG_TAG,
          "No Channel ID specified (in AndroidManifest.xml or as firstParameter for init method), fetching config via Package Name: "
              + getContext().getPackageName());
      // get channel config
      getChannelConfigFromBundleId(
          "/channel-config?bundleId=" + getContext().getPackageName() + "&platformName=Android", autoRegister);
    }

    // increment app opens
    incrementAppOpens();

    setUpNotificationCategoryGroups();

    deleteDataBasedOnRetentionDays();
  }

  public SessionListener initSessionListener() {
    return open -> {
      try {
        this.isAppOpen = open;
        if (open) {
          this.trackSessionStart();

          if (this.pendingRequestLocationPermissionCall) {
            this.requestLocationPermission();
          }

          if (this.pendingRequestNotificationPermissionCall && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this.requestNotificationPermission(this.getCurrentActivity());
          }

          if (this.pendingInitFeaturesCall) {
            this.initFeatures();
          }

          if (this.appBannerModule != null && this.getCurrentActivity() != null) {
            this.appBannerModule.initSession(channelId);
          } else if (this.getCurrentActivity() == null) {
            Logger.e(LOG_TAG, "getCurrentActivity() is null");
          }

          if (this.pendingPageViews.size() > 0) {
            for (PageView pageView : this.pendingPageViews) {
              this.trackPageView(pageView.getUrl(), pageView.getParams());
            }
            this.pendingPageViews = new ArrayList<>();
          }

          if (isAutoResubscribe()
                  && (getSubscriptionId(CleverPush.context) == null || getSubscriptionId(CleverPush.context).isEmpty())
                  && areNotificationsEnabled()) {
            Logger.d(LOG_TAG, "autoResubscribe");
            subscribe();
          }
        } else {
          this.trackSessionEnd();
        }
      } catch (Exception e) {
        Logger.e(LOG_TAG, "Error in initSessionListener.", e);
      }
    };
  }

  public void setNotificationReceivedListener(
      @Nullable final NotificationReceivedListenerBase notificationReceivedListener) {
    this.notificationReceivedListener = notificationReceivedListener;
  }

  public void setNotificationOpenedListener(@Nullable final NotificationOpenedListenerBase notificationOpenedListener) {
    this.notificationOpenedListener = notificationOpenedListener;

    if (notificationOpenedListener instanceof NotificationOpenedCallbackListener
        && !notificationOpenShouldStartActivity()) {
      Logger.d(LOG_TAG,
          "The NotificationOpenedCallbackListener is supposed to be used with the notification_open_activity_disabled meta data option, which is not being used at the moment.");
    }

    // fire listeners for unprocessed open notifications
    if (this.notificationOpenedListener != null) {
      for (NotificationOpenedResult result : getUnprocessedOpenedNotifications()) {
        fireNotificationOpenedListener(result);
        result.setNotificationOpenedActivity(null); // Make sure we have no garbage collection problems
      }
      unprocessedOpenedNotifications.clear();
    }
  }

  public void setSubscribedListener(@Nullable final SubscribedListener subscribedListener) {
    this.subscribedListener = subscribedListener;
  }

  public void getChannelConfigFromChannelId(boolean autoRegister, String storedChannelId, String storedSubscriptionId) {
    CleverPush instance = this;
    String configPath = "/channel/" + this.channelId + "/config";
    if (developmentMode) {
      configPath += "?t=" + System.currentTimeMillis();
    }
    CleverPushHttpClient.getWithRetry(configPath,
        new ChannelConfigFromChannelIdResponseHandler(instance).getResponseHandler(autoRegister, storedChannelId,
            storedSubscriptionId));
  }

  public void getChannelConfigFromBundleId(String url, boolean autoRegister) {
    CleverPush instance = this;
    CleverPushHttpClient.getWithRetry(url,
        new ChannelConfigFromBundleIdResponseHandler(instance, initializeListener).getResponseHandler(autoRegister));
  }

  public void incrementAppOpens() {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
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
   *
   * @param autoRegister boolean for auto register
   */
  public void subscribeOrSync(boolean autoRegister) {
    subscribeOrSync(autoRegister, false);
  }

  public void subscribeOrSync(boolean autoRegister, boolean isChannelIdChanged) {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    sharedPreferences.edit().putString(CleverPushPreferences.CHANNEL_ID, getChannelId(getContext())).apply();

    String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);

    if (shouldAutoSubscribe(sharedPreferences, autoRegister, subscriptionId)) {
      boolean newSubscription = subscriptionId == null;
      this.subscribe(newSubscription, new SubscribedCallbackListener() {
        @Override
        public void onSuccess(String subscriptionId) {
          initFeatures();
        }

        @Override
        public void onFailure(Throwable exception) {
          initFeatures();
        }
      });
    } else if (subscriptionId != null && !this.areNotificationsEnabled()
        && !this.ignoreDisabledNotificationPermission) {
      Logger.d(LOG_TAG, "notification authorization revoked, unsubscribing");
      this.unsubscribe(new UnsubscribedListener() {
        @Override
        public void onSuccess() {
          initFeatures();
        }

        @Override
        public void onFailure(Throwable throwable) {
          initFeatures();
        }
      });
    } else {
      if (subscriptionId != null) {
        Date nextSyncDate = new Date(getNextSync(sharedPreferences) * MILLISECONDS_PER_SECOND);
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT_ISO, Locale.getDefault());
        String formattedDate = dateFormat.format(nextSyncDate);
        Logger.d(LOG_TAG, "Subscribed with ID (next sync at " + formattedDate + "): " + subscriptionId);

        this.getSubscriptionManager().checkChangedPushToken(channelConfig);
      }
      this.fireSubscribedListener(subscriptionId);
      this.setSubscriptionId(subscriptionId);
      initFeatures(isChannelIdChanged);
    }
  }

  private int getNextSync(SharedPreferences sharedPreferences) {
    int lastSync = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0);
    return lastSync + SYNC_SUBSCRIPTION_INTERVAL;
  }

  private boolean shouldAutoSubscribe(SharedPreferences sharedPreferences, boolean autoRegister,
                                      String subscriptionId) {
    int nextSync = getNextSync(sharedPreferences);
    boolean isUnsubscribed = sharedPreferences.getBoolean(CleverPushPreferences.UNSUBSCRIBED, false);
    return (!isUnsubscribed && subscriptionId == null && autoRegister) || isSyncTimePassed(nextSync, subscriptionId);
  }

  private boolean isSyncTimePassed(int nextSync, String subscriptionId) {
    int currentTime = (int) (System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
    return subscriptionId != null && nextSync < currentTime;
  }

  /**
   * initialize the features
   */
  public void initFeatures() {
    initFeatures(false);
  }

  public void initFeatures(boolean isChannelIdChanged) {
    try {
      if (getCurrentActivity() == null) {
        this.pendingInitFeaturesCall = true;
        return;
      }

      Logger.d(LOG_TAG, "initFeatures");

      this.pendingInitFeaturesCall = false;

      if (this.isSubscribed()) {
        this.showTopicDialogOnNewAdded();
      }
      this.initAppReview();
      this.initGeoFences();

      appBannerModule = getAppBannerModule();

      if (pendingAppBannerTrackingEnabled != null) {
        appBannerModule.setTrackingEnabled(pendingAppBannerTrackingEnabled);
        pendingAppBannerTrackingEnabled = null;
      }

      if (getPendingAppBannerEvents() != null) {
        for (TriggeredEvent event : getPendingAppBannerEvents()) {
          appBannerModule.triggerEvent(event);
        }
        pendingAppBannerEvents = null;
      }

      if (getPendingShowAppBannerId() != null) {
        appBannerModule.showBanner(pendingShowAppBannerId, pendingShowAppBannerNotificationId);
        pendingShowAppBannerId = null;
        pendingShowAppBannerNotificationId = null;
      }

      appBannerModule.initSession(channelId, isChannelIdChanged);

      BroadcastReceiverUtils.registerReceiver(this);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in initFeatures.", e);
    }
  }

  /**
   * initialize the App review
   */
  void initAppReview() {
    this.getChannelConfig(config -> {
      if (config != null && config.optBoolean("appReviewEnabled")) {
        try {
          SharedPreferences sharedPreferences = getSharedPreferences(getContext());

          if (sharedPreferences.getLong(CleverPushPreferences.APP_REVIEW_SHOWN, 0) == 0) {
            int appReviewSeconds = config.optInt("appReviewSeconds", 0);
            int appReviewOpens = config.optInt("appReviewOpens", 0);
            int appReviewDays = config.optInt("appReviewDays", 0);

            long currentUnixTime = System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
            long allowedUnixTime =
                sharedPreferences.getLong(CleverPushPreferences.SUBSCRIPTION_CREATED_AT, 0) + (appReviewDays
                    * SECONDS_PER_DAY);
            int appOpens = sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 1);

            if (currentUnixTime >= allowedUnixTime && appOpens >= appReviewOpens) {
              (getCurrentActivity()).runOnUiThread(() -> {
                getHandler().postDelayed(() -> {
                  if (getSharedPreferences(getContext()).getLong(CleverPushPreferences.APP_REVIEW_SHOWN, 0) == 0) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putLong(CleverPushPreferences.APP_REVIEW_SHOWN,
                        System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
                    editor.apply();

                    showFiveStarsDialog(config);
                  }
                }, appReviewSeconds * MILLISECONDS_PER_SECOND);
              });
            }
          }
        } catch (Exception ex) {
          Logger.d(LOG_TAG, "Error in initAppReview.", ex);
        }
      }
    });
  }

  void showFiveStarsDialog(JSONObject config) {
    try {
      FiveStarsDialog dialog = new FiveStarsDialog(
              getCurrentActivity(),
              config.optString("appReviewEmail")
      );
      dialog.setRateText(config.optString("appReviewText"))
              .setTitle(config.optString("appReviewTitle"))
              .setPositiveButtonText(config.optString("appReviewYes"))
              .setNegativeButtonText(config.optString("appReviewLater"))
              .setNeverButtonText(config.optString("appReviewNo"))
              .setForceMode(false)
              .show();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in displaying FiveStarsDialog.", e);
    }
  }

  private void requestPermission(Activity dialogActivity, String permissionType,
                                 PermissionActivity.PermissionCallback callback) {
    try {
      if (dialogActivity == null || dialogActivity.getClass().equals(PermissionActivity.class)) {
        return;
      }

      Logger.d(LOG_TAG, "Requesting permission: " + permissionType);

      PermissionActivity.registerAsCallback(permissionType, callback);

      Intent intent = new Intent(dialogActivity, PermissionActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
      intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
      intent.putExtra(PermissionActivity.INTENT_EXTRA_PERMISSION_TYPE, permissionType);
      dialogActivity.startActivity(intent);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Exception during permission request.", e);
    }
  }

  public void requestLocationPermission() {
    this.requestLocationPermission(getCurrentActivity());
  }

  public void requestLocationPermission(Activity dialogActivity) {
    if (this.hasLocationPermission()) {
      return;
    }
    if (dialogActivity == null) {
      this.pendingRequestLocationPermissionCall = true;
      return;
    }
    this.pendingRequestLocationPermissionCall = false;
    this.requestPermission(dialogActivity, Manifest.permission.ACCESS_FINE_LOCATION,
        new PermissionActivity.PermissionCallback() {
          @Override
          public void onGrant() {
            if (geofenceList == null || geofenceList.size() == 0) {
              initGeoFences();
            }
          }

          @Override
          public void onDeny() {

          }
        });
  }

  /**
   * request for push notification
   */
  @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
  public void requestNotificationPermission(Activity dialogActivity) {

    if (this.hasNotificationPermission()) {
      return;
    }
    if (dialogActivity == null) {
      this.pendingRequestNotificationPermissionCall = true;
      return;
    }
    this.pendingRequestNotificationPermissionCall = false;
    this.requestPermission(dialogActivity, Manifest.permission.POST_NOTIFICATIONS,
        new PermissionActivity.PermissionCallback() {
          @Override
          public void onGrant() {
            if (!ignoreDisabledNotificationPermission) {
              subscribe(true, pendingSubscribeCallbackListener);
            }
          }

          @Override
          public void onDeny() {
            if (!ignoreDisabledNotificationPermission) {
              String error =
                  "Can not subscribe because the notification permission has been denied by the user. You can call CleverPush.setIgnoreDisabledNotificationPermission(true) to still allow subscriptions, e.g. for silent pushes.";
              Logger.d(LOG_TAG, error);
              if (pendingSubscribeCallbackListener != null) {
                pendingSubscribeCallbackListener.onFailure(new Exception(error));
              }
            }
          }
        });
  }

  /**
   * to check if app has location permission
   */
  public boolean hasLocationPermission() {
    return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED;
  }

  /**
   * to check if app has notification permission
   */
  @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
  public boolean hasNotificationPermission() {
    return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.POST_NOTIFICATIONS)
        == PackageManager.PERMISSION_GRANTED;
  }

  private void savePreferencesMap(String mapKey, Map<String, Integer> inputMap) {
    Logger.d(LOG_TAG, "savePreferencesMap: " + mapKey + " - " + inputMap.toString());
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
      Logger.e(LOG_TAG, "Error while parsing JSON in loadPreferencesMap.", e);
    }
    Logger.d(LOG_TAG, "loadPreferencesMap: " + mapKey + " - " + outputMap.toString());
    return outputMap;
  }

  void checkTags(String urlStr, Map<String, ?> params) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);

    try {
      URL url = new URL(urlStr);
      String pathname = url.getPath();
      Logger.d(LOG_TAG, "checkTags: " + pathname);

      this.getAvailableTags(tags -> {
        if (tags != null) {
          CleverPush self = this;
          for (ChannelTag tag : tags) {
            TagsMatcher.autoAssignTagMatches(getCurrentActivity(), tag, pathname, params, matches -> {
              if (matches) {
                Logger.d(LOG_TAG, "checkTag: matches: YES - " + tag.getName());

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
                      }, tag.getAutoAssignSeconds() * MILLISECONDS_PER_SECOND);
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
                Logger.d(LOG_TAG, "checkTag: matches: NO - " + tag.getName());
              }
            });
          }
        }
      });
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in checkTags.", e);
    }
  }

  /**
   * initialize Geo Fences
   */
  public void initGeoFences() {
    if (hasLocationPermission()) {
      googleApiClient = getGoogleApiClient();
      getChannelConfig(config -> {
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
                          geoFence.getLong("radius"))
                      .setExpirationDuration(Geofence.NEVER_EXPIRE) // Future: use "endsAt" instead
                      .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                      .build());
                }
              }
            }
          } catch (Exception ex) {
            Logger.d(LOG_TAG, "Error in initGeoFences.", ex);
          }
        }
      });

      if (geofenceList.size() > 0) {
        googleApiClient.connect();
      }
    }
  }

  GoogleApiClient getGoogleApiClient() {
    return new GoogleApiClient.Builder(CleverPush.context)
        .addConnectionCallbacks(getConnectionCallbacks())
        .addOnConnectionFailedListener(getOnConnectionFailedListener())
        .addApi(LocationServices.API)
        .build();
  }

  private GoogleApiClient.OnConnectionFailedListener getOnConnectionFailedListener() {
    return connectionResult -> Logger.d(LOG_TAG, "GoogleApiClient onConnectionFailed");
  }

  private GeofencingRequest getGeofencingRequest() {
    GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
    builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
    builder.addGeofences(geofenceList);
    return builder.build();
  }

  private PendingIntent getGeofencePendingIntent() {
    if (geofencePendingIntent != null) {
      return geofencePendingIntent;
    }
    Intent intent = new Intent(CleverPush.context, GeofenceBroadcastReceiver.class);
    geofencePendingIntent = PendingIntent.getBroadcast(CleverPush.context, 0, intent,
        PendingIntent.FLAG_MUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    return geofencePendingIntent;
  }

  private GoogleApiClient.ConnectionCallbacks getConnectionCallbacks() {
    return new GoogleApiClient.ConnectionCallbacks() {
      @Override
      @SuppressWarnings({"MissingPermission"})
      public void onConnected(@Nullable Bundle bundle) {
        try {
          Logger.d(LOG_TAG, "GoogleApiClient onConnected");

          if (getCurrentActivity() != null) {
            if (geofenceList.size() > 0) {
              geofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                      .addOnSuccessListener(getCurrentActivity(), new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                        }
                      })
                      .addOnFailureListener(getCurrentActivity(), new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                        }
                      });
            }
          } else {
            Logger.e(LOG_TAG, "getConnectionCallbacks onConnected getCurrentActivity() is null");
          }
        } catch (Exception e) {
          Logger.e(LOG_TAG, "Error in getConnectionCallbacks onConnected", e);
        }
      }

      @Override
      public void onConnectionSuspended(int i) {
        Logger.d(LOG_TAG, "GoogleApiClient onConnectionSuspended");
      }
    };
  }

  public void trackPageView(String url) {
    this.trackPageView(url, null);
  }

  public void trackPageView(String url, Map<String, ?> params) {
    try {
      if (getCurrentActivity() == null) {
        this.pendingPageViews.add(new PageView(url, params));
        return;
      }

      this.currentPageUrl = url;

      this.checkTags(url, params);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in trackPageView.", e);
    }
  }

  public void autoTrackWebViewPages(String url) {
    trackPageView(url);
  }

  public void setWebViewClientListener(WebView webView, WebViewClientListener webViewClientListener,
                                       Map<String, ?> params) {
    this.webViewClientListener = webViewClientListener;
    webView.setWebViewClient(new CleverPushWebViewClient(params, webViewClientListener, this));
  }

  void trackSessionStart() {
    Logger.d(LOG_TAG, "trackSessionStart");
    this.sessionVisits = 0;
    this.sessionStartedTimestamp = System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
    this.waitForTrackingConsent(() -> this.getChannelConfig(config -> {
      if (config != null && config.optBoolean("trackAppStatistics") || getSubscriptionId(getContext()) != null) {
        updateServerSessionStart();
      }
    }));
  }

  public void updateServerSessionStart() {
    isSessionStartCalled = true;
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    String fcmToken = sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null);
    String lastNotificationId = sharedPreferences.getString(CleverPushPreferences.LAST_NOTIFICATION_ID, null);

    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("channelId", getChannelId(getContext()));
      jsonBody.put("subscriptionId", getSubscriptionId(getContext()));
      jsonBody.put("fcmToken", fcmToken);
      jsonBody.put("lastNotificationId", lastNotificationId);
    } catch (JSONException ex) {
      Logger.e(LOG_TAG, "Error creating updateServerSessionStart request parameter", ex);
    }

    CleverPushHttpClient.postWithRetry("/subscription/session/start", jsonBody,
        new TrackSessionStartResponseHandler().getResponseHandler());
  }

  public void increaseSessionVisits() {
    this.sessionVisits += 1;
  }

  void trackSessionEnd() {
    Logger.d(LOG_TAG, "trackSessionEnd");
    if (getSessionStartedTimestamp() == 0) {
      Logger.e(LOG_TAG, "Error tracking session end - session started timestamp is 0");
      return;
    }

    this.waitForTrackingConsent(() -> this.getChannelConfig(config -> {
      if (config != null && config.optBoolean("trackAppStatistics") || getSubscriptionId(getContext()) != null) {
        updateServerSessionEnd();
      }

      this.sessionStartedTimestamp = 0;
      this.sessionVisits = 0;
      this.lastClickedNotificationId = null;
    }));
  }

  public void updateServerSessionEnd() {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    String fcmToken = sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null);
    long sessionEndedTimestamp = System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
    long sessionDuration = sessionEndedTimestamp - getSessionStartedTimestamp();

    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("channelId", getChannelId(getContext()));
      jsonBody.put("subscriptionId", getSubscriptionId(getContext()));
      jsonBody.put("fcmToken", fcmToken);
      jsonBody.put("visits", getSessionVisits());
      jsonBody.put("duration", sessionDuration);
    } catch (JSONException ex) {
      Logger.e(LOG_TAG, "Error creating updateServerSessionEnd request parameter", ex);
    }

    CleverPushHttpClient.postWithRetry("/subscription/session/end", jsonBody, new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d(LOG_TAG, "Session ended");
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e(LOG_TAG, "Failed to end session." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e(LOG_TAG, "Failed to end session." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    });
  }

  public boolean isSubscribed() {
    return getSharedPreferences(getContext()).contains(CleverPushPreferences.SUBSCRIPTION_ID);
  }

  public void subscribe() {
    subscribe(false);
  }

  private void subscribe(boolean newSubscription) {
    subscribe(newSubscription, null, getCurrentActivity());
  }

  public void subscribe(SubscribedCallbackListener subscribedCallbackListener) {
    subscribe(false, subscribedCallbackListener, getCurrentActivity());
  }

  public void subscribe(SubscribedCallbackListener subscribedCallbackListener, Activity dialogActivity) {
    subscribe(false, subscribedCallbackListener, dialogActivity);
  }

  private void subscribe(boolean newSubscription, SubscribedCallbackListener subscribedCallbackListener) {
    subscribe(newSubscription, subscribedCallbackListener, getCurrentActivity());
  }

  private void subscribe(boolean newSubscription, SubscribedCallbackListener subscribedCallbackListener,
                         Activity dialogActivity) {
    if (getIabTcfMode() != null && getIabTcfMode() == IabTcfMode.SUBSCRIBE_WAIT_FOR_CONSENT) {
      this.waitForSubscribeConsent(() -> {
        handleSubscribe(newSubscription, subscribedCallbackListener, dialogActivity);
      });
    } else {
      handleSubscribe(newSubscription, subscribedCallbackListener, dialogActivity);
    }
  }

  private void handleSubscribe(boolean newSubscription, SubscribedCallbackListener subscribedCallbackListener,
                               Activity dialogActivity) {
    try {
      if (isSubscriptionInProgress()) {
        if (subscribedCallbackListener != null) {
          subscribedCallbackListener.onFailure(new Exception("Subscription is already in progress"));
        }
        return;
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !this.hasNotificationPermission() && shouldAutoRequestNotificationPermission()) {
        if (!this.ignoreDisabledNotificationPermission) {
          this.pendingSubscribeCallbackListener = subscribedCallbackListener;
        }

        this.requestNotificationPermission(dialogActivity);

        if (!this.ignoreDisabledNotificationPermission) {
          return;
        }
      }

      if (!this.areNotificationsEnabled() && !this.ignoreDisabledNotificationPermission) {
        String error =
                "Can not subscribe because notifications have been disabled by the user. You can call CleverPush.setIgnoreDisabledNotificationPermission(true) to still allow subscriptions, e.g. for silent pushes.";
        Logger.d(LOG_TAG, error);

        if (subscribedCallbackListener != null) {
          subscribedCallbackListener.onFailure(new Exception(error));
        }
        return;
      }

      this.subscriptionInProgress = true;
      SharedPreferences.Editor editor = getSharedPreferences(getContext()).edit();
      editor.putBoolean(CleverPushPreferences.UNSUBSCRIBED, false);
      editor.apply();

      this.getChannelConfig(config -> {
        SubscriptionManager subscriptionManager = this.getSubscriptionManager();
        CleverPush self = this;
        subscriptionManager.subscribe(config, new SubscribedCallbackListener() {
          @Override
          public void onSuccess(String newSubscriptionId) {
            self.subscriptionInProgress = false;
            Logger.d(LOG_TAG, "subscribed with ID: " + newSubscriptionId);

            if (!isSessionStartCalled) {
              self.trackSessionStart();
            }

            self.fireSubscribedListener(newSubscriptionId);
            self.setSubscriptionId(newSubscriptionId);

            if (subscribedCallbackListener != null) {
              subscribedCallbackListener.onSuccess(newSubscriptionId);
            }

            if (!isSubscribeForTopicsDialog) {
              if (newSubscriptionId != null && newSubscription) {
                if (config != null && !config.optBoolean("confirmAlertHideChannelTopics", false)) {
                  JSONArray channelTopics = config.optJSONArray("channelTopics");
                  if (channelTopics != null && channelTopics.length() > 0) {
                    Set<String> topics = self.getSubscriptionTopics();
                    if (topics == null || topics.size() == 0) {
                      self.setSubscriptionTopics(setUpSelectedTopicIds(channelTopics).toArray(new String[0]));
                    }
                    updatePendingTopicsDialog(true);
                  }
                }

                if (!isConfirmAlertShown()) {
                  // If the confirm alert has not been tracked by the customer already,
                  // we will track it here retroperspectively to ensure opt-in rate statistics
                  // are correct
                  self.setConfirmAlertShown();
                }
              }
            }
          }

          @Override
          public void onFailure(Throwable exception) {
            self.subscriptionInProgress = false;

            if (subscribedCallbackListener != null) {
              subscribedCallbackListener.onFailure(exception);
            }
          }
        });
      });
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in handleSubscribe.", e);
    }
  }

  public void updatePendingTopicsDialog(Boolean pendingTopicsDialog) {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, pendingTopicsDialog);
    editor.apply();
    CleverPush.instance.showPendingTopicsDialog();
  }

  private Set<String> setUpSelectedTopicIds(JSONArray channelTopics) {
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
    return selectedTopicIds;
  }

  public void unsubscribe() {
    this.unsubscribe(null);
  }

  public void unsubscribe(UnsubscribedListener listener) {
    String subscriptionId = getSubscriptionId(getContext());
    if (subscriptionId != null && !subscriptionId.isEmpty()) {
      JSONObject jsonBody = getJsonObject();
      try {
        jsonBody.put("channelId", getChannelId(getContext()));
        jsonBody.put("subscriptionId", subscriptionId);
      } catch (JSONException e) {
        Logger.e(LOG_TAG, "Error creating unsubscribe request parameter", e);
      }

      CleverPushHttpClient.postWithRetry("/subscription/unsubscribe", jsonBody,
              new UnsubscribeResponseHandler(this, listener).getResponseHandler());
    } else {
      Logger.d(LOG_TAG, "unsubscribe: There is no subscription for CleverPush SDK.");
      clearSubscriptionData();
    }
  }

  JSONObject getJsonObject() {
    return new JSONObject();
  }

  JSONObject getJsonObject(String jsonString) {
    try {
      return new JSONObject(jsonString);
    } catch (JSONException exception) {
      Logger.d(LOG_TAG, "Error while creating JSONObject from string", exception);
    }
    return null;
  }

  public void setTrackingConsentRequired(Boolean required) {
    trackingConsentRequired = required;
  }

  public void waitForTrackingConsent(TrackingConsentListener listener) {
    if (listener != null) {
      if (isTrackingConsentRequired() && !hasTrackingConsent()) {
        if (!hasTrackingConsentCalled() || (hasTrackingConsentCalled() && !hasTrackingConsent())) {
          getTrackingConsentListeners().add(listener);
        }
      } else {
        listener.ready();
      }
    }
  }

  public void setTrackingConsent(Boolean consent) {
    boolean previousTrackingConsent = hasTrackingConsent;
    hasTrackingConsentCalled = true;
    hasTrackingConsent = consent;

    if (!hasTrackingConsent && previousTrackingConsent) {
      // hasTrackingConsent was true before, so call the removal method
      removeSubscriptionTagsAndAttributes();
      stopCampaigns(null);
    }

    // hasTrackingConsent is false then event should not be stored in the queue for TCF
    if (getIabTcfMode() != null && getIabTcfMode() != IabTcfMode.DISABLED && !previousTrackingConsent && hasTrackingConsent) {
      trackingConsentListeners = new ArrayList<>();
    }

    if (hasTrackingConsent) {
      for (TrackingConsentListener listener : trackingConsentListeners) {
        listener.ready();
      }
    }

    if (isTrackingConsentRequired() && !hasTrackingConsent && trackingConsentListeners.size() > 0) {
      return;
    }

    trackingConsentListeners = new ArrayList<>();
  }

  public void stopCampaigns(StopCampaignListener listener) {
    String subscriptionId = getSubscriptionId(getContext());
    if (subscriptionId != null && !subscriptionId.isEmpty()) {
      JSONObject jsonBody = getJsonObject();
      try {
        jsonBody.put("channelId", getChannelId(getContext()));
        jsonBody.put("subscriptionId", subscriptionId);
      } catch (JSONException e) {
        Logger.e(LOG_TAG, "Error creating stopCampaigns request parameter", e);
      }

      CleverPushHttpClient.postWithRetry("/subscription/stop-campaigns", jsonBody,
              new StopCampaignResponseHandler(this, listener).getResponseHandler());
    } else {
      Logger.d(LOG_TAG, "stopCampaigns: There is no subscription for CleverPush SDK.");
    }
  }

  private void removeSubscriptionTagsAndAttributes() {
    try {
      if (getSubscriptionId(CleverPush.context) == null || getSubscriptionId(CleverPush.context).isEmpty()) {
        Logger.d(LOG_TAG, "removeSubscriptionTagsAndAttributes: There is no subscription for CleverPush SDK.");
        return;
      }
      Set<String> subscribedTagIds = this.getSubscriptionTags();
      Map<String, Object> subscriptionAttributes = this.getSubscriptionAttributes();

      if (subscribedTagIds != null && subscribedTagIds.size() > 0) {
        String[] tagIdsArray = subscribedTagIds.toArray(new String[0]);
        if (getIabTcfMode() != null && getIabTcfMode() != IabTcfMode.DISABLED) {
          removeSubscriptionTagTrackingImplementation(null, tagIdsArray);
        } else {
          removeSubscriptionTags(tagIdsArray);
        }
      }

      if (subscriptionAttributes != null && subscriptionAttributes.size() > 0) {
        for (Map.Entry<String, Object> entry : subscriptionAttributes.entrySet()) {
          String key = entry.getKey();
          Object value = entry.getValue();

          if (value instanceof String) {
            if (getIabTcfMode() != null && getIabTcfMode() != IabTcfMode.DISABLED) {
              this.setSubscriptionAttributeObjectImplementation(key, "", new SetSubscriptionAttributeResponseHandler());
            } else {
              this.setSubscriptionAttribute(key, "");
            }
          } else {
            if (getIabTcfMode() != null && getIabTcfMode() != IabTcfMode.DISABLED) {
              this.setSubscriptionAttributeObjectImplementation(key, new String[0], new SetSubscriptionAttributeResponseHandler());
            } else {
              this.setSubscriptionAttribute(key, new String[0]);
            }
          }
        }
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error at removeSubscriptionTagsAndAttributes.", e);
    }
  }

  public void setSubscribeConsentRequired(Boolean required) {
    subscribeConsentRequired = required;
  }

  public void waitForSubscribeConsent(SubscribeConsentListener listener) {
    if (listener != null) {
      if (isSubscribeConsentRequired() && !hasSubscribeConsent()) {
        if (!hasSubscribeConsentCalled() || (hasSubscribeConsentCalled() && !hasSubscribeConsent())) {
          getSubscribeConsentListeners().add(listener);
        }
      } else {
        listener.ready();
      }
    }
  }

  public void setSubscribeConsent(Boolean consent) {
    boolean previousSubscribeConsent = hasSubscribeConsent;
    hasSubscribeConsentCalled = true;
    hasSubscribeConsent = consent;

    // hasSubscribeConsent is false then event should not be stored in the queue for TCF
    if (getIabTcfMode() != null && getIabTcfMode() != IabTcfMode.DISABLED && !previousSubscribeConsent && hasSubscribeConsent) {
      subscribeConsentListeners = new ArrayList<>();
    }

    if (hasSubscribeConsent) {
      for (SubscribeConsentListener listener : subscribeConsentListeners) {
        listener.ready();
      }
    }

    if (isSubscribeConsentRequired() && !hasSubscribeConsent && subscribeConsentListeners.size() > 0) {
      return;
    }

    subscribeConsentListeners = new ArrayList<>();
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

  public synchronized static void getChannelConfig(ChannelConfigListener listener) {
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

  public synchronized void getSubscriptionId(SubscribedListener listener) {
    if (listener != null) {
      if (subscriptionId == null || subscriptionId.isEmpty()) {
        Logger.d(LOG_TAG, "getSubscriptionId: There is no subscription for CleverPush SDK.");
        getSubscriptionIdListeners.add(listener);
      } else {
        listener.subscribed(subscriptionId);
      }
    }
  }

  public void getDeviceToken(DeviceTokenListener listener) {
    if (listener != null) {
      SharedPreferences sharedPreferences = getSharedPreferences(getContext());
      String fcmToken = sharedPreferences.getString(CleverPushPreferences.FCM_TOKEN, null);
      listener.complete(fcmToken);
    }
  }

  public synchronized void setSubscriptionId(String value) {
    subscriptionId = value;
    notifyAll();

    if (subscriptionId != null) {
      for (SubscribedListener listener : getSubscriptionIdListeners) {
        if (listener == null) {
          continue;
        }
        listener.subscribed(subscriptionId);
      }
      getSubscriptionIdListeners = new ArrayList<>();
    } else {
      Logger.d(LOG_TAG, "setSubscriptionId: There is no subscription for CleverPush SDK.");
    }
  }

  public boolean isNotificationReceivedListenerCallback() {
    return notificationReceivedListener != null
        && notificationReceivedListener instanceof NotificationReceivedCallbackListener;
  }

  public boolean isUsingNotificationOpenedCallbackListener() {
    return notificationOpenedListener != null
        && notificationOpenedListener instanceof NotificationOpenedCallbackListener;
  }

  public boolean fireNotificationReceivedListener(final NotificationOpenedResult openedResult) {
    if (notificationReceivedListener == null) {
      return false;
    }
    notificationReceivedListener.notificationReceived(openedResult);
    return true;
  }

  public boolean fireNotificationOpenedListener(final NotificationOpenedResult openedResult) {
    if (openedResult.getNotification().getAppBanner() != null) {
      getActivityLifecycleListener().setActivityInitializedListener(new ActivityInitializedListener() {
        @Override
        public void initialized() {
          showAppBanner(openedResult.getNotification().getAppBanner(),
              openedResult.getNotification().getId());
        }
      });
    }

    if (notificationOpenedListener == null) {
      unprocessedOpenedNotifications.add(openedResult);
      return false;
    }

    if (notificationOpenedListener instanceof NotificationOpenedCallbackListener) {
      ((NotificationOpenedCallbackListener) notificationOpenedListener).notificationOpenedCallback(openedResult,
          openedResult.getNotificationOpenedActivity());
    } else {
      notificationOpenedListener.notificationOpened(openedResult);
    }

    return true;
  }

  public boolean fireNotificationReceivedCallbackListener(final NotificationOpenedResult openedResult) {
    if (notificationReceivedListener == null
        || !(notificationReceivedListener instanceof NotificationReceivedCallbackListener)) {
      return false;
    }
    return ((NotificationReceivedCallbackListener) notificationReceivedListener).notificationReceivedCallback(
        openedResult);
  }

  public void removeNotificationReceivedListener() {
    notificationReceivedListener = null;
  }

  public void removeNotificationOpenedListener() {
    notificationOpenedListener = null;
  }

  public void fireSubscribedListener(final String subscriptionId) {
    if (getSubscribedListener() == null || subscriptionId == null) {
      return;
    }
    getSubscribedListener().subscribed(subscriptionId);
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
      PackageInfo info =
          pm.getPackageInfo(GoogleApiAvailability.GOOGLE_PLAY_SERVICES_PACKAGE, PackageManager.GET_META_DATA);
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
    return getSharedPreferences(getContext()).getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
  }

  public Set<String> getSubscriptionTopics() {
    return getSharedPreferences(getContext()).getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>());
  }

  public boolean hasSubscriptionTopic(String topicId) {
    return this.getSubscriptionTopics().contains(topicId);
  }

  public boolean hasSubscriptionTopics() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    return sharedPreferences.contains(CleverPushPreferences.SUBSCRIPTION_TOPICS);
  }

  public boolean hasDeSelectAll() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    return sharedPreferences.getBoolean(CleverPushPreferences.SUBSCRIPTION_TOPICS_DESELECT_ALL, false);
  }

  public void setDeSelectAll(Boolean deSelectAll) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(CleverPushPreferences.SUBSCRIPTION_TOPICS_DESELECT_ALL, deSelectAll);
    editor.apply();
  }

  public Map<String, Object> getSubscriptionAttributes() {
    Map<String, Object> outputMap = new HashMap<>();
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    try {
      if (sharedPreferences != null) {
        String jsonString =
            sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString());
        JSONObject jsonObject = getJsonObject(jsonString);
        Iterator<String> keysItr = jsonObject.keys();
        while (keysItr.hasNext()) {
          String k = keysItr.next();
          Object v = jsonObject.get(k);
          outputMap.put(k, v);
        }
      }
    } catch (Exception ex) {
      Logger.e(LOG_TAG, "Error while getting subscription attribute.", ex);
    }
    return outputMap;
  }

  public boolean hasSubscriptionTag(String tagId) {
    return this.getSubscriptionTags().contains(tagId);
  }

  public Object getSubscriptionAttribute(String attributeId) {
    return this.getSubscriptionAttributes().get(attributeId);
  }

  public Set<ChannelTag> getAvailableTagsFromConfig(JSONObject channelConfig) {
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
        Logger.d(LOG_TAG, "Error while getting available tags from channel config.", ex);
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

  Set<CustomAttribute> getAvailableAttributesFromConfig(JSONObject channelConfig) {
    Set<CustomAttribute> attributes = new HashSet<>();
    if (channelConfig != null && channelConfig.has("customAttributes")) {
      try {
        JSONArray attributesArray = channelConfig.getJSONArray("customAttributes");
        if (attributesArray != null) {
          for (int i = 0; i < attributesArray.length(); i++) {
            JSONObject attributeObject = attributesArray.getJSONObject(i);
            if (attributeObject != null) {
              CustomAttribute attribute =
                  new CustomAttribute(attributeObject.getString("id"), attributeObject.getString("name"));
              attributes.add(attribute);
            }
          }
        }
      } catch (JSONException ex) {
        Logger.d(LOG_TAG,  "Error while getting available attributes from channel config.", ex);
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
              } catch (Exception e) {
                Logger.e(LOG_TAG, "Error parsing customData for topic.", e);
              }

              ChannelTopic topic = new ChannelTopic(
                  topicObject.getString("_id"),
                  topicObject.optString("name"),
                  topicObject.optString("parentTopic", null),
                  topicObject.optBoolean("defaultUnchecked", false),
                  topicObject.optString("fcmBroadcastTopic", null),
                  topicObject.optString("externalId", null),
                  customData);
              topics.add(topic);
            }
          }
        }
      } catch (JSONException ex) {
        Logger.e(LOG_TAG, "Error while getting available topics from channel config.", ex);
      }
    }
    return topics;
  }

  public void addSubscriptionTags(String[] tagIds) {
    addSubscriptionTagTrackingConsent(null, tagIds);
  }

  public void addSubscriptionTopic(String topicId) {
    addSubscriptionTopic(topicId, (CompletionListener) null);
  }

  public void addSubscriptionTopic(String topicId, CompletionListener completionListener) {
    addSubscriptionTopic(topicId, new CompletionFailureListener() {
      @Override
      public void onComplete() {
        if (completionListener != null) {
          completionListener.onComplete();
        }
      }

      @Override
      public void onFailure(Exception exception) {

      }
    });
  }

  public void addSubscriptionTopic(String topicId, CompletionFailureListener completionListener) {
    this.getSubscriptionId(subscriptionId -> {
      if (subscriptionId == null || subscriptionId.isEmpty()) {
        Logger.d(LOG_TAG, "addSubscriptionTopic: There is no subscription for CleverPush SDK.");
        return;
      }
      Set<String> topics = this.getSubscriptionTopics();
      if (topics.contains(topicId)) {
        if (completionListener != null) {
          completionListener.onComplete();
        }
        return;
      }
      topics.add(topicId);
      List<String> list = new ArrayList<>(topics);
      String[] topicsArray = list.toArray(new String[0]);
      this.setSubscriptionTopics(topicsArray);

      JSONObject jsonBody = new JSONObject();
      try {
        jsonBody.put("channelId", getChannelId(getContext()));
        jsonBody.put("topicId", topicId);
        jsonBody.put("subscriptionId", subscriptionId);
      } catch (JSONException ex) {
        Logger.e(LOG_TAG, "Error creating addSubscriptionTopic request parameter", ex);
      }
      CleverPushHttpClient.ResponseHandler responseHandler =
          new SetSubscriptionTopicsResponseHandler(this).getResponseHandler(topicsArray, completionListener);
      CleverPushHttpClient.postWithRetry(
          "/subscription/topic/add",
          jsonBody,
          responseHandler);
    });
  }

  public void removeSubscriptionTopic(String topicId) {
    removeSubscriptionTopic(topicId, (CompletionListener) null);
  }

  public void removeSubscriptionTopic(String topicId, CompletionListener listener) {
    removeSubscriptionTopic(topicId, new CompletionFailureListener() {
      @Override
      public void onComplete() {
        if (listener != null) {
          listener.onComplete();
        }
      }

      @Override
      public void onFailure(Exception exception) {

      }
    });
  }

  public void removeSubscriptionTopic(String topicId, CompletionFailureListener completionListener) {
    this.getSubscriptionId(subscriptionId -> {
      if (subscriptionId == null || subscriptionId.isEmpty()) {
        Logger.d(LOG_TAG, "removeSubscriptionTopic: There is no subscription for CleverPush SDK.");
        return;
      }
      Set<String> topics = this.getSubscriptionTopics();
      if (!topics.contains(topicId)) {
        if (completionListener != null) {
          completionListener.onComplete();
        }
        return;
      }
      topics.remove(topicId);
      List<String> list = new ArrayList<>(topics);
      String[] topicsArray = list.toArray(new String[0]);
      this.setSubscriptionTopics(topicsArray);

      JSONObject jsonBody = new JSONObject();
      try {
        jsonBody.put("channelId", getChannelId(getContext()));
        jsonBody.put("topicId", topicId);
        jsonBody.put("subscriptionId", subscriptionId);
      } catch (JSONException ex) {

        Logger.e(LOG_TAG, "Error creating removeSubscriptionTopic request parameter", ex);
      }
      CleverPushHttpClient.ResponseHandler responseHandler =
          new SetSubscriptionTopicsResponseHandler(this).getResponseHandler(topicsArray, completionListener);
      CleverPushHttpClient.postWithRetry("/subscription/topic/remove",
          jsonBody,
          responseHandler);
    });
  }

  public void addSubscriptionTag(String tagId) {
    addSubscriptionTagTrackingConsent(null, tagId);
  }

  public void addSubscriptionTag(String tagId, CompletionFailureListener listener) {
    addSubscriptionTagTrackingConsent(listener, tagId);
  }

  private void addSubscriptionTagTrackingConsent(CompletionFailureListener listener, String... tagIds) {
    startTrackingConsent(getAddSubscriptionTagsSubscribedListener(listener, tagIds));
  }

  private SubscribedListener getAddSubscriptionTagsSubscribedListener(CompletionFailureListener completionListener,
                                                                      String... tagIds) {
    return subscriptionId -> {
      if (addSubscriptionTagsHelper != null && !addSubscriptionTagsHelper.isFinished()) {
        addSubscriptionTagsHelper.addTagIds(tagIds);
        return;
      }
      addSubscriptionTagsHelper =
          new AddSubscriptionTags(subscriptionId, this.channelId, getSharedPreferences(getContext()),
              completionListener, tagIds);
      addSubscriptionTagsHelper.addSubscriptionTags();
    };
  }

  public void removeSubscriptionTags(String[] tagIds) {
    removeSubscriptionTagTrackingConsent(null, tagIds);
  }

  public void removeSubscriptionTag(String tagId) {
    removeSubscriptionTagTrackingConsent(null, tagId);
  }

  public void removeSubscriptionTag(String tagId, CompletionFailureListener listener) {
    removeSubscriptionTagTrackingConsent(listener, tagId);
  }

  private void removeSubscriptionTagTrackingConsent(CompletionFailureListener listener, String... tagIds) {
    startTrackingConsent(getRemoveSubscriptionTagSubscribedListener(listener, tagIds));
  }

  private SubscribedListener getRemoveSubscriptionTagSubscribedListener(CompletionFailureListener listener,
                                                                        String... tagIds) {
    return subscriptionId -> {
      if (removeSubscriptionTagsHelper != null && !removeSubscriptionTagsHelper.isFinished()) {
        removeSubscriptionTagsHelper.addTagIds(tagIds);
        return;
      }
      removeSubscriptionTagsHelper =
          new RemoveSubscriptionTags(subscriptionId, this.channelId, getSharedPreferences(getContext()), listener,
              tagIds);
      removeSubscriptionTagsHelper.removeSubscriptionTags();
    };
  }

  private void startTrackingConsent(SubscribedListener subscribedListener) {
    this.waitForTrackingConsent(() -> new Thread(() -> this.getSubscriptionId(subscribedListener)).start());
  }

  private void removeSubscriptionTagTrackingImplementation(CompletionFailureListener listener,
                                                           String... tagIds) {
    this.getSubscriptionId(subscriptionId -> {
      if (subscriptionId != null && !subscriptionId.isEmpty()) {
        if (removeSubscriptionTagsHelper != null && !removeSubscriptionTagsHelper.isFinished()) {
          removeSubscriptionTagsHelper.addTagIds(tagIds);
          return;
        }
        removeSubscriptionTagsHelper =
                new RemoveSubscriptionTags(subscriptionId, this.channelId, getSharedPreferences(getContext()), listener,
                        tagIds);
        removeSubscriptionTagsHelper.removeSubscriptionTags();
      } else {
        Logger.d(LOG_TAG, "removeSubscriptionTagTrackingImplementation: There is no subscription for CleverPush SDK.");
      }
    });
  }

  public void setSubscriptionTopics(String[] topicIds) {
    setSubscriptionTopics(topicIds, (CompletionListener) null);
  }

  public void setSubscriptionTopics(String[] topicIds, CompletionListener completionListener) {
    setSubscriptionTopics(topicIds, new CompletionFailureListener() {
      @Override
      public void onComplete() {
        if (completionListener != null) {
          completionListener.onComplete();
        }
      }

      @Override
      public void onFailure(Exception exception) {

      }
    });
  }

  public void setSubscriptionTopics(String[] topicIds, CompletionFailureListener completionListener) {
    new Thread(() -> {
      SharedPreferences sharedPreferences = getSharedPreferences(getContext());
      final int topicsVersion = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, 0) + 1;
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.remove(CleverPushPreferences.SUBSCRIPTION_TOPICS);
      editor.apply();
      editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>(Arrays.asList(topicIds)));
      editor.putInt(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION, topicsVersion);
      editor.apply();

      this.getSubscriptionId(subscriptionId -> {
        if (subscriptionId != null && !subscriptionId.isEmpty()) {
          JSONObject jsonBody = new JSONObject();
          try {
            JSONArray topicsArray = new JSONArray();
            for (String topicId : topicIds) {
              topicsArray.put(topicId);
            }

            jsonBody.put("channelId", getChannelId(getContext()));
            jsonBody.put("topics", topicsArray);
            jsonBody.put("topicsVersion", topicsVersion);
            jsonBody.put("subscriptionId", subscriptionId);
          } catch (JSONException ex) {
            Logger.e(LOG_TAG, "Error creating setSubscriptionTopics request parameter", ex);
          }

          Logger.d(LOG_TAG, "setSubscriptionTopics: " + Arrays.toString(topicIds));

          CleverPushHttpClient.postWithRetry("/subscription/sync/" + getChannelId(getContext()), jsonBody,
              new SetSubscriptionTopicsResponseHandler(this).getResponseHandler(topicIds, completionListener));
        } else {
          Logger.d(LOG_TAG, "setSubscriptionTopics: There is no subscription for CleverPush SDK.");
        }
      });
    }).start();
  }

  public void setSubscriptionAttribute(String attributeId, String value) {
    setSubscriptionAttribute(attributeId, value, new SetSubscriptionAttributeResponseHandler());
  }

  public void setSubscriptionAttribute(String attributeId, String value, SetSubscriptionAttributeResponseHandler responseHandler) {
    this.setSubscriptionAttributeObject(attributeId, value, responseHandler);
  }

  public void setSubscriptionAttribute(String attributeId, String[] values) {
    setSubscriptionAttribute(attributeId, values, new SetSubscriptionAttributeResponseHandler());
  }

  public void setSubscriptionAttribute(String attributeId, String[] values, SetSubscriptionAttributeResponseHandler responseHandler) {
    this.setSubscriptionAttributeObject(attributeId, values, responseHandler);
  }

  private void setSubscriptionAttributeObject(String attributeId, Object value, SetSubscriptionAttributeResponseHandler responseHandler) {
    this.waitForTrackingConsent(() -> new Thread(() -> this.setSubscriptionAttributeObjectImplementation(attributeId, value, responseHandler)).start());
  }

  private void setSubscriptionAttributeObjectImplementation(String attributeId, Object value, SetSubscriptionAttributeResponseHandler responseHandler) {
    this.getSubscriptionId(subscriptionId -> {
      if (subscriptionId != null && !subscriptionId.isEmpty()) {
        JSONObject jsonBody = getJsonObject();
        try {
          jsonBody.put("channelId", getChannelId(getContext()));
          jsonBody.put("attributeId", attributeId);
          if (value instanceof String) {
            jsonBody.put("value", value);
          } else if (value instanceof String[]) {
            JSONArray jsonArray = new JSONArray();
            for (String attributeValue : (String[]) value) {
              jsonArray.put(attributeValue);
            }
            jsonBody.put("value", jsonArray);
          }
          jsonBody.put("subscriptionId", subscriptionId);
        } catch (JSONException ex) {
          Logger.e(LOG_TAG, "Error creating setSubscriptionAttributeObject request parameter", ex);
        }

        Map<String, Object> subscriptionAttributes = this.getSubscriptionAttributes();
        subscriptionAttributes.put(attributeId, value);
        CleverPushHttpClient.postWithRetry("/subscription/attribute", jsonBody, responseHandler.getResponseHandler(subscriptionAttributes));
      } else {
        Logger.d(LOG_TAG, "setSubscriptionAttribute: There is no subscription for CleverPush SDK.");
      }
    });
  }

  public void pushSubscriptionAttributeValue(String attributeId, String value) {
    this.waitForTrackingConsent(() -> new Thread(() -> this.getSubscriptionId(subscriptionId -> {
      if (subscriptionId != null && !subscriptionId.isEmpty()) {
        JSONObject jsonBody = new JSONObject();
        try {
          jsonBody.put("channelId", this.channelId);
          jsonBody.put("attributeId", attributeId);
          jsonBody.put("value", value);
          jsonBody.put("subscriptionId", subscriptionId);
        } catch (JSONException ex) {
          Logger.e(LOG_TAG,  "Error creating pushSubscriptionAttributeValue request parameter", ex);
        }

        Map<String, Object> subscriptionAttributes = this.getSubscriptionAttributes();

        ArrayList<String> arrayList = new ArrayList<>();
        try {
          JSONArray arrayValue = (JSONArray) subscriptionAttributes.get(attributeId);
          Mapper<JSONArray, Collection<String>> jsonArrayToListMapper = new SubscriptionToListMapper();
          arrayList.addAll(jsonArrayToListMapper.toValue(arrayValue));
        } catch (Exception ex) {
          Logger.e(LOG_TAG, "pushSubscriptionAttributeValue: Error processing attribute values", ex);
        }

        arrayList.add(value);
        String[] arrayString = arrayList.toArray(new String[0]);
        if (arrayString == null) {
          return;
        }
        subscriptionAttributes.put(attributeId, arrayString);

        CleverPushHttpClient.postWithRetry("/subscription/attribute/push-value", jsonBody,
            pushSubscriptionAttributeValueResponseHandler(subscriptionAttributes));
      } else {
        Logger.d(LOG_TAG, "pushSubscriptionAttributeValue: There is no subscription for CleverPush SDK.");
      }
    })).start());
  }

  private CleverPushHttpClient.ResponseHandler pushSubscriptionAttributeValueResponseHandler(
      Map<String, Object> subscriptionAttributes) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        try {
          if (sharedPreferences != null) {
            JSONObject jsonObject = new JSONObject(subscriptionAttributes);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES).apply();
            editor.putString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, jsonString);
            editor.apply();
          }
        } catch (Exception ex) {
          Logger.e(LOG_TAG,  "pushSubscriptionAttributeValueResponseHandler onSuccess: Error updating subscription attributes in SharedPreferences", ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e(LOG_TAG, "Error pushing attribute value" +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e(LOG_TAG, "Error pushing attribute value" +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };
  }

  public void pullSubscriptionAttributeValue(String attributeId, String value) {
    this.waitForTrackingConsent(() -> new Thread(() -> this.getSubscriptionId(subscriptionId -> {
      if (subscriptionId != null && !subscriptionId.isEmpty()) {
        JSONObject jsonBody = new JSONObject();
        try {
          jsonBody.put("channelId", this.channelId);
          jsonBody.put("attributeId", attributeId);
          jsonBody.put("value", value);
          jsonBody.put("subscriptionId", subscriptionId);
        } catch (JSONException ex) {
          Logger.e(LOG_TAG, "Error creating pullSubscriptionAttributeValue request parameter", ex);
        }

        Map<String, Object> subscriptionAttributes = this.getSubscriptionAttributes();

        ArrayList<String> arrayList = new ArrayList<>();

        try {
          JSONArray arrayValue = (JSONArray) subscriptionAttributes.get(attributeId);
          Mapper<JSONArray, Collection<String>> jsonArrayToListMapper = new SubscriptionToListMapper();
          arrayList.addAll(jsonArrayToListMapper.toValue(arrayValue));
        } catch (Exception ex) {
          Logger.e(LOG_TAG, "pullSubscriptionAttributeValue: Error processing attribute values", ex);
        }
        arrayList.remove(value);

        String[] arrayString = arrayList.toArray(new String[0]);
        if (arrayString == null) {
          return;
        }
        subscriptionAttributes.put(attributeId, arrayString);

        CleverPushHttpClient.postWithRetry("/subscription/attribute/pull-value", jsonBody,
            pullSubscriptionAttributeValueResponseHandler(subscriptionAttributes));
      } else {
        Logger.d(LOG_TAG, "pullSubscriptionAttributeValue: There is no subscription for CleverPush SDK.");
      }
    })).start());
  }

  private CleverPushHttpClient.ResponseHandler pullSubscriptionAttributeValueResponseHandler(
      Map<String, Object> subscriptionAttributes) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        try {
          if (sharedPreferences != null) {
            JSONObject jsonObject = new JSONObject(subscriptionAttributes);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES).apply();
            editor.putString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, jsonString);
            editor.apply();
          }
        } catch (Exception ex) {
          Logger.e(LOG_TAG, "pullSubscriptionAttributeValueResponseHandler onSuccess: Error updating subscription attributes in SharedPreferences", ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e(LOG_TAG, "Error pulling attribute value" +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e(LOG_TAG, "Error pulling attribute value" +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };
  }

  public boolean hasSubscriptionAttributeValue(String attributeId, String value) {
    Map<String, Object> subscriptionAttributes = this.getSubscriptionAttributes();

    ArrayList<String> arrayList = new ArrayList<>();
    try {
      JSONArray arrayValue = (JSONArray) subscriptionAttributes.get(attributeId);
      Mapper<JSONArray, Collection<String>> jsonArrayToListMapper = new SubscriptionToListMapper();
      arrayList.addAll(jsonArrayToListMapper.toValue(arrayValue));
    } catch (Exception ex) {
      Logger.e(LOG_TAG, "hasSubscriptionAttributeValue: Error checking subscription attribute value", ex);
    }

    return arrayList.contains(value);
  }

  public void trySubscriptionSync() {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    int currentTime = (int) (System.currentTimeMillis() / MILLISECONDS_PER_SECOND);
    int lastSync = sharedPreferences.getInt(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC, 0);
    int nextSync = lastSync + 5; // allow sync every 5s
    String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
    if (!isSubscriptionInProgress() && subscriptionId != null && nextSync < currentTime) {
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
      editor.apply();

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
      editor.apply();

      this.trySubscriptionSync();
    }
  }

  public Set<Notification> getNotifications() {
    return StoredNotificationsService.getNotifications(getSharedPreferences(getContext()));
  }

  public void getNotifications(NotificationsCallbackListener notificationsCallbackListener) {
    StoredNotificationsService.getNotifications(getSharedPreferences(getContext()), notificationsCallbackListener);
  }

  public void setMaximumNotificationCount(int limit) {
    NotificationDataProcessor.maximumNotifications = limit;
  }

  public void getNotifications(@Deprecated boolean combineWithApi,
                               NotificationsCallbackListener notificationsCallbackListener) {
    StoredNotificationsService.getNotifications(getSharedPreferences(getContext()), notificationsCallbackListener);
  }

  public StoredNotificationsCursor getCombinedNotifications(int pageSize) {
    return StoredNotificationsService.getCombinedNotifications(this.channelId, getSharedPreferences(getContext()),
        pageSize);
  }

  public void removeNotification(String notificationId) {
    List<Notification> notifications = new ArrayList<Notification>(
        StoredNotificationsService.getNotificationsFromLocal(getSharedPreferences(getContext())));
    for (int i = 0; i < notifications.size(); i++) {
      if (notificationId.equalsIgnoreCase(notifications.get(i).id)) {
        notifications.remove(i);
      }
    }
    getSharedPreferences(getContext()).edit().putString(CleverPushPreferences.NOTIFICATIONS_JSON,
        new Gson().toJson(notifications, new TypeToken<List<Notification>>() {
        }.getType())).apply();
  }

  public void trackEvent(String eventName) {
    this.trackEvent(eventName, (Map<String, Object>) null);
  }

  public void trackEvent(String eventName, Float amount) {
    Map<String, Object> properties = new HashMap<>();
    properties.put("amount", amount);
    this.trackEvent(eventName, properties);
  }

  public void trackEvent(String eventName, Map<String, Object> properties) {
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
          Logger.e(LOG_TAG, "Event not found");
          return;
        }

        String eventId = event.optString("_id");

        this.waitForTrackingConsent(() -> {
          this.getSubscriptionId(subscriptionId -> {
            if (subscriptionId != null && !subscriptionId.isEmpty()) {
              JSONObject jsonBody = new JSONObject();

              try {
                JSONObject propertiesObject = new JSONObject();
                if (properties != null) {
                  for (Map.Entry<String, Object> entry : properties.entrySet()) {
                    propertiesObject.put(entry.getKey(), entry.getValue());
                  }
                }

                jsonBody.put("channelId", this.channelId);
                jsonBody.put("eventId", eventId);
                jsonBody.put("properties", propertiesObject);
                jsonBody.put("subscriptionId", subscriptionId);

                if (this.lastClickedNotificationId != null) {
                  jsonBody.put("notificationId", this.lastClickedNotificationId);
                }
              } catch (JSONException ex) {
                Logger.e(LOG_TAG, "Error creating trackEvent request parameter", ex);
              }

              CleverPushHttpClient.postWithRetry("/subscription/conversion", jsonBody,
                      new TrackEventResponseHandler().getResponseHandler(eventName));
            } else {
              Logger.d(LOG_TAG, "trackEvent: There is no subscription for CleverPush SDK.");
            }
          });
        });

        ArrayList<TableBannerTrackEvent> bannerTrackEvents = (ArrayList<TableBannerTrackEvent>) DatabaseClient.getInstance(CleverPush.context).
                getAppDatabase()
                .trackEventDao()
                .getBannerTrackEvent(eventId);
        if (bannerTrackEvents.size() > 0) {
          DatabaseClient.getInstance(CleverPush.context)
                  .getAppDatabase()
                  .trackEventDao()
                  .increaseCount(eventId, getCurrentDateTime());
        }

        TriggeredEvent triggeredEvent = new TriggeredEvent(eventId, properties);
        if (getAppBannerModule() == null) {
          pendingAppBannerEvents.add(triggeredEvent);
          return;
        }
        getAppBannerModule().triggerEvent(triggeredEvent);
      } catch (Exception ex) {
        String message = "Track event failed because of error";
        Logger.e(LOG_TAG, message, ex);
      }
    });
  }

  public void triggerFollowUpEvent(String eventName) {
    this.triggerFollowUpEvent(eventName, null);
  }

  public void triggerFollowUpEvent(String eventName, Map<String, String> parameters) {
    this.waitForTrackingConsent(() -> {
      try {
        this.getSubscriptionId(subscriptionId -> {
          if (subscriptionId != null && !subscriptionId.isEmpty()) {
            JSONObject jsonParameters = new JSONObject();
            if (parameters != null) {
              for (Map.Entry<String, String> entry : parameters.entrySet()) {
                try {
                  jsonParameters.put(entry.getKey(), entry.getValue());
                } catch (JSONException ex) {
                  Logger.e(LOG_TAG, ex.getMessage(), ex);
                }
              }
            }

            JSONObject jsonBody = new JSONObject();
            try {
              jsonBody.put("channelId", this.channelId);
              jsonBody.put("name", eventName);
              jsonBody.put("parameters", jsonParameters);
              jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
              Logger.e(LOG_TAG, "Error creating triggerFollowUpEvent request parameter", ex);
            }

            CleverPushHttpClient.postWithRetry("/subscription/event", jsonBody,
                new TriggerFollowUpEventResponseHandler().getResponseHandler(eventName));
          } else {
            Logger.d(LOG_TAG, "triggerFollowUpEvent: There is no subscription for CleverPush SDK.");
          }
        });
      } catch (Exception ex) {
        Logger.e(LOG_TAG, "Error in triggerFollowUpEvent", ex);
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
      Logger.e(LOG_TAG, "Error creating trackNotificationDelivered request parameter", e);
    }

    CleverPushHttpClient.post("/notification/delivered", jsonBody, null);
  }

  public void trackNotificationClicked(String notificationId) {
    this.getSubscriptionId(subscriptionId -> this.trackNotificationClicked(notificationId, subscriptionId));
  }

  public void trackNotificationClicked(String notificationId, String subscriptionId) {
    trackNotificationClicked(notificationId, subscriptionId, null, null);
  }

  public void trackNotificationClicked(String notificationId, String subscriptionId, String channelId, String actionIndex) {
    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("notificationId", notificationId);
      jsonBody.put("subscriptionId", subscriptionId);
      jsonBody.put("channelId", channelId);
      if (actionIndex != null && !actionIndex.isEmpty()) {
        jsonBody.put("action", actionIndex);
      }
    } catch (JSONException e) {
      Logger.e(LOG_TAG, "Error creating trackNotificationClicked request parameter", e);
    }

    CleverPushHttpClient.post("/notification/clicked", jsonBody, null);

    lastClickedNotificationId = notificationId;
  }

  public void showAppBanner(String bannerId) {
    showAppBanner(bannerId, null, true);
  }

  public void showAppBanner(String bannerId, String notificationId, boolean force) {
    if (appBannerModule == null) {
      pendingShowAppBannerId = bannerId;
      pendingShowAppBannerNotificationId = notificationId;
      return;
    }
    appBannerModule.showBanner(bannerId, notificationId, force);
  }

  public void showAppBanner(String bannerId, String notificationId) {
    showAppBanner(bannerId, notificationId, false);
  }

  /**
   * This method may be called by the customer to ensure opt-in rates get calculated correctly.
   */
  public void setConfirmAlertShown() {
    confirmAlertShown = true;

    JSONObject jsonBody = new JSONObject();
    try {
      jsonBody.put("channelId", channelId);
      jsonBody.put("platformName", "Android");
      jsonBody.put("browserType", "SDK");
    } catch (JSONException e) {
      Logger.e(LOG_TAG, "Error creating channel confirm-alert request parameter.", e);
    }

    CleverPushHttpClient.postWithRetry("/channel/confirm-alert", jsonBody, null);
  }

  void showPendingTopicsDialog() {
    this.getChannelConfig(config -> {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
      try {
        if (config != null && sharedPreferences.getBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, false)) {
          final int topicsDialogSeconds = config.optInt("topicsDialogMinimumSeconds", 0);
          int topicsDialogSessions = config.optInt("topicsDialogMinimumSessions", 0);
          int topicsDialogDays = config.optInt("topicsDialogMinimumDays", 0);

          long currentUnixTime = System.currentTimeMillis() / MILLISECONDS_PER_SECOND;
          long allowedUnixTime =
              sharedPreferences.getLong(CleverPushPreferences.SUBSCRIPTION_CREATED_AT, 0) + (topicsDialogDays
                  * SECONDS_PER_DAY);
          int appOpens = sharedPreferences.getInt(CleverPushPreferences.APP_OPENS, 1);

          if (currentUnixTime >= allowedUnixTime && appOpens >= topicsDialogSessions) {
            getCurrentActivity().runOnUiThread(() -> {
              new Handler().postDelayed(() -> {
                if (sharedPreferences.getBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, false)) {
                  SharedPreferences.Editor editor = sharedPreferences.edit();
                  editor.putBoolean(CleverPushPreferences.PENDING_TOPICS_DIALOG, false);
                  editor.apply();

                  this.showTopicsDialog();
                }
              }, topicsDialogSeconds * MILLISECONDS_PER_SECOND);
            });
          }
        }
      } catch (Exception ex) {
        Logger.d(LOG_TAG, "Error in showPendingTopicsDialog.", ex);
      }
    });
  }

  public void showTopicsDialog() {
    getActivityLifecycleListener().setActivityInitializedListener(() -> showTopicsDialog(getCurrentActivity()));
  }

  public void showTopicsDialog(Activity dialogActivity) {
    showTopicsDialog(dialogActivity, null);
  }

  public void showTopicsDialog(Activity dialogActivity, TopicsDialogListener topicsDialogListener) {
    try {
      if (android.os.Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
        showTopicsDialog(dialogActivity, topicsDialogListener, R.style.Theme_AppCompat_Dialog_Alert);
      } else {
        showTopicsDialog(dialogActivity, topicsDialogListener, R.style.cleverpush_topics_dialog_theme_overlay);
      }
    } catch (IllegalStateException ex) {
      showTopicsDialog(dialogActivity, topicsDialogListener, R.style.Theme_AppCompat_Dialog_Alert);
    }
  }

  public void showTopicsDialog(Activity dialogActivity, TopicsDialogListener topicsDialogListener,
                               @StyleRes int themeResId) {
    showTopicsDialog(dialogActivity, topicsDialogListener, themeResId, false);
  }

  public void showTopicsDialog(Activity dialogActivity, TopicsDialogListener topicsDialogListener,
                               @StyleRes int themeResId, boolean isRecursiveCall) {
    try {
      // Ensure it will only be shown once at a time
      if (isShowingTopicsDialog()) {
        return;
      }

      this.getChannelConfig(channelConfig -> {
        if (channelConfig == null) {
          if (topicsDialogListener != null) {
            topicsDialogListener.callback(false);
          }
          showingTopicsDialog = false;
          return;
        }

        JSONArray channelTopics = channelConfig.optJSONArray("channelTopics");
        topicsDialogShowWhenNewAdded = channelConfig.optBoolean("topicsDialogShowWhenNewAdded");

        if (channelTopics == null || channelTopics.length() == 0) {
          Logger.w(LOG_TAG,
                  "CleverPush: showTopicsDialog: No topics found. Create some first in the CleverPush channel settings.");
          return;
        }

        dialogActivity.runOnUiThread(() -> {
          if (!instance.hasSubscriptionTopics()) {
            instance.setSubscriptionTopics(setUpSelectedTopicIds(channelTopics).toArray(new String[0]));
          }

          Set<String> selectedTopics = new HashSet<>(instance.getSubscriptionTopics());
          final boolean hasDeSelectAllInitial = this.hasDeSelectAll();
          boolean showUnsubscribeCheckbox = channelConfig.optBoolean("topicsDialogShowUnsubscribe", false);

          LinearLayout checkboxLayout = getTopicCheckboxLayout();
          LinearLayout parentLayout = getTopicParentLayout();

          ScrollView scrollView = new ScrollView(context);
          LinearLayout.LayoutParams scrollViewLayoutParams =
                  new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                          LinearLayout.LayoutParams.WRAP_CONTENT);
          scrollViewLayoutParams.setMargins(45, 45, 45, 45);
          scrollView.setLayoutParams(scrollViewLayoutParams);
          scrollView.addView(parentLayout);

          CheckBox unsubscribeCheckbox = null;
          if (showUnsubscribeCheckbox) {
            unsubscribeCheckbox = getTopicCheckbox(getNightModeFlags(), context.getText(R.string.deselect_all));
          }

          setTopicCheckboxList(parentLayout, channelTopics, unsubscribeCheckbox, hasDeSelectAll(), getNightModeFlags(),
                  selectedTopics);

          checkboxLayout.addView(scrollView);

          try {
            AlertDialog.Builder alertBuilder =
                    getTopicAlertBuilder(dialogActivity, topicsDialogListener, themeResId, channelConfig, channelTopics,
                            hasDeSelectAllInitial, checkboxLayout, unsubscribeCheckbox, selectedTopics);
            AlertDialog topicDialogAlert = alertBuilder.create();
            topicDialogAlert.setOnShowListener(dialog -> {
              Logger.d(LOG_TAG, "showTopicsDialog activity: " + dialogActivity.getClass().getCanonicalName());
              showingTopicsDialog = true;
            });
            if (topicDialogAlert != null) {
              topicDialogAlert.show();
            }
          } catch (Exception e) {
            Logger.d(LOG_TAG, "Error creating topic dialog alert", e);
            if (!isRecursiveCall) {
              showTopicsDialog(dialogActivity, topicsDialogListener, R.style.cleverpush_topics_dialog_theme, true);
            }
          }
        });

      });
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in showTopicsDialog", e);
    }
  }

  private int getNightModeFlags() {
    return getContext().getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
  }

  private AlertDialog.Builder getTopicAlertBuilder(Context dialogActivity, TopicsDialogListener topicsDialogListener,
                                                   int themeResId, JSONObject channelConfig, JSONArray channelTopics,
                                                   boolean hasDeSelectAllInitial, LinearLayout checkboxLayout,
                                                   CheckBox unsubscribeCheckbox, Set<String> selectedTopics) {
    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(dialogActivity, themeResId);

    String headerTitle = null;
    if (channelConfig.has("confirmAlertSelectTopicsLaterTitle")) {
      try {
        headerTitle = channelConfig.getString("confirmAlertSelectTopicsLaterTitle");
      } catch (Exception e) {
        Logger.e(LOG_TAG, "Error while getting header title from channel config.", e);
      }
    }
    if (headerTitle == null || headerTitle.isEmpty()) {
      headerTitle = CleverPush.context.getResources().getString(R.string.topics_dialog_title);
    }

    alertBuilder.setTitle(headerTitle);
    alertBuilder.setView(checkboxLayout);

    alertBuilder.setOnDismissListener(dialogInterface -> {
      updateTopicLastCheckedTime();
      showingTopicsDialog = false;
    });

    alertBuilder.setNegativeButton(CleverPush.context.getResources().getString(R.string.cancel),
        (dialogInterface, i) -> {
          if (topicsDialogListener != null) {
            topicsDialogListener.callback(false);
          }
          showingTopicsDialog = false;
        });

    alertBuilder.setPositiveButton(CleverPush.context.getResources().getString(R.string.save), (dialogInterface, i) -> {
      if (unsubscribeCheckbox != null && unsubscribeCheckbox.isChecked()) {
        unsubscribe();
        setDeSelectAll(true);
        selectedTopics.clear();
        for (int j = 0; j < channelTopics.length(); j++) {
          try {
            JSONObject channelTopic = (JSONObject) channelTopics.get(j);
            channelTopic.put("defaultUnchecked", true);
          } catch (JSONException e) {
            Logger.e(LOG_TAG, "getTopicAlertBuilder: Error while updating channelTopics.", e);
          }
        }
      } else {
        if (unsubscribeCheckbox != null && hasDeSelectAllInitial) {
          setDeSelectAll(false);
          subscriptionId = null;
          isSubscribeForTopicsDialog = true;
          this.subscribe(true);
        } else if (unsubscribeCheckbox == null && subscriptionId == null) {
          isSubscribeForTopicsDialog = true;
          this.subscribe(true);
        }

        CleverPush.getInstance(CleverPush.context).setSubscriptionTopics(selectedTopics.toArray(new String[0]));
      }

      dialogInterface.dismiss();

      if (topicsDialogListener != null) {
        topicsDialogListener.callback(true);
      }
      showingTopicsDialog = false;
    });
    return alertBuilder;
  }

  private CheckBox getTopicCheckbox(int nightModeFlags, CharSequence text) {
    CheckBox checkBox = new CheckBox(CleverPush.context);
    checkBox.setText(text);
    if (!this.disableNightModeAdaption && nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
      checkBox.setTextColor(Color.WHITE);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        checkBox.setButtonTintList(ColorStateList.valueOf(Color.WHITE));
      }
    }
    return checkBox;
  }

  private LinearLayout getTopicParentLayout() {
    LinearLayout parentLayout = new LinearLayout(context);
    LinearLayout.LayoutParams parentLayoutParams =
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    parentLayout.setLayoutParams(parentLayoutParams);
    parentLayout.setOrientation(LinearLayout.VERTICAL);
    return parentLayout;
  }

  private LinearLayout getTopicCheckboxLayout() {
    LinearLayout checkboxLayout = new LinearLayout(context);
    checkboxLayout.setLayoutParams(
        new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
    checkboxLayout.setOrientation(LinearLayout.VERTICAL);
    return checkboxLayout;
  }

  /**
   * Will create list of checkbox for the topics.
   *
   * @param parentLayout   parent layout to add checkboxes
   * @param channelTopics  topics from the channel
   * @param deselectAll    is deselectall checkbox is checked or not
   * @param nightModeFlags flag if there is night mode
   * @param selectedTopics selectedTopics
   */
  void setTopicCheckboxList(LinearLayout parentLayout, JSONArray channelTopics, CheckBox unsubscribeCheckbox,
                            boolean deselectAll, int nightModeFlags, Set<String> selectedTopics) {
    parentLayout.removeAllViews();

    if (unsubscribeCheckbox != null) {
      unsubscribeCheckbox.setChecked(deselectAll);
      parentLayout.addView(unsubscribeCheckbox);
      unsubscribeCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked) {
          setTopicCheckboxList(parentLayout, channelTopics, unsubscribeCheckbox, true, nightModeFlags, selectedTopics);
        }
      });
    }

    setupTopicParentCheckboxes(parentLayout, unsubscribeCheckbox, channelTopics, deselectAll, nightModeFlags,
        selectedTopics);
  }

  private boolean isTopicCheckboxChecked(Set<String> selectedTopics, JSONObject topic) {
    String id = topic.optString("_id");
    boolean defaultUnchecked = false;
    try {
      defaultUnchecked = topic.optBoolean("defaultUnchecked");
    } catch (Exception ignored) {
    }
    boolean defaultUncheckedAndEmptyTopics = selectedTopics.size() == 0 && !defaultUnchecked && !this.isSubscribed();
    return defaultUncheckedAndEmptyTopics || selectedTopics.contains(id);
  }

  public void setupTopicParentCheckboxes(LinearLayout parentLayout, CheckBox unsubscribeCheckbox,
                                         JSONArray channelTopics, boolean deselectAll, int nightModeFlags,
                                         Set<String> selectedTopics) {
    try {
      for (int i = 0; i < channelTopics.length(); i++) {
        JSONObject topic = channelTopics.getJSONObject(i);
        if (topic != null) {
          String id = topic.getString("_id");

          CheckBox checkbox = getTopicCheckbox(nightModeFlags, getTopicCheckboxText(topic));
          if (!hasDeSelectAll() && !deselectAll) {
            checkbox.setChecked(this.isTopicCheckboxChecked(selectedTopics, topic));
          }

          if (topic.has("parentTopic") && topic.optString("parentTopic").length() > 0) {
            continue;
          }

          parentLayout.addView(checkbox);
          setupTopicChildCheckboxes(parentLayout, unsubscribeCheckbox, channelTopics, deselectAll, nightModeFlags,
              selectedTopics, id, checkbox);
        } else {
          Logger.e(LOG_TAG, "Topic is null at index " + i);
        }
      }
    } catch (JSONException e) {
      Logger.e(LOG_TAG, "Error while setting up parent topics checkboxes.", e);
    }
  }

  private void toggleCheckedTopic(Set<String> selectedTopics, String id, boolean isChecked,
                                  CheckBox unsubscribeCheckbox, Set<String> childTopicIds) {
    if (isChecked) {
      selectedTopics.add(id);
      if (unsubscribeCheckbox != null) {
        unsubscribeCheckbox.setChecked(false);
      }
    } else {
      selectedTopics.remove(id);
      if (childTopicIds != null) {
        for (String childId : childTopicIds) {
          selectedTopics.remove(childId);
        }
      }
    }
    if (selectedTopics.size() == 0 && unsubscribeCheckbox != null) {
      unsubscribeCheckbox.setChecked(true);
    }
  }

  private void setupTopicChildCheckboxes(LinearLayout parentLayout, CheckBox unsubscribeCheckbox,
                                         JSONArray channelTopics, boolean deselectAll, int nightModeFlags,
                                         Set<String> selectedTopics, String id, CheckBox checkbox) {
    try {
      Set<String> childTopicIds = new HashSet<>();
      final LinearLayout childLayout = new LinearLayout(context);
      for (int j = 0; j < channelTopics.length(); j++) {
        JSONObject childTopic = channelTopics.getJSONObject(j);
        if (childTopic != null && childTopic.optString("parentTopic").equals(id)) {
          String childId = childTopic.optString("_id");
          childTopicIds.add(childId);

          LinearLayout.LayoutParams childLayoutParams =
              new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                  LinearLayout.LayoutParams.WRAP_CONTENT);
          childLayoutParams.setMargins(65, 0, 0, 20);
          childLayout.setLayoutParams(childLayoutParams);
          childLayout.setOrientation(LinearLayout.VERTICAL);

          CheckBox checkboxChild = getTopicCheckbox(nightModeFlags, getTopicCheckboxText(childTopic));

          checkboxChild.setOnCheckedChangeListener((buttonView, isChecked) -> {
            toggleCheckedTopic(selectedTopics, childId, isChecked, unsubscribeCheckbox, null);
          });

          if (!hasDeSelectAll() && !deselectAll) {
            checkboxChild.setChecked(this.isTopicCheckboxChecked(selectedTopics, childTopic));
          }

          childLayout.setVisibility(checkbox.isChecked() ? View.VISIBLE : View.GONE);
          childLayout.addView(checkboxChild);
        }
      }

      final boolean hasChildren = childTopicIds.size() > 0;
      checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
        toggleCheckedTopic(selectedTopics, id, isChecked, unsubscribeCheckbox, childTopicIds);
        if (hasChildren) {
          childLayout.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        }
      });

      if (childTopicIds.size() > 0) {
        parentLayout.addView(childLayout);
      }
    } catch (JSONException e) {
      Logger.e(LOG_TAG, "Error while setting up child topics checkboxes.", e);
    }
  }

  private void updateTopicLastCheckedTime() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    sharedPreferences.edit()
        .putInt(CleverPushPreferences.TOPIC_LAST_CHECKED, (int) (System.currentTimeMillis() / 1000L)).apply();
  }

  private String getTopicCheckboxText(JSONObject topic) {
    int oneHour = 60 * 60;
    int topicLastChecked = getTopicLastChecked();
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
      simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      Date date = simpleDateFormat.parse(topic.optString("createdAt"));
      int topicCreatedAt = (int) (date.getTime() / 1000);
      if (this.isSubscribed() && topicsDialogShowWhenNewAdded && topicLastChecked > 0 && topicCreatedAt + oneHour > topicLastChecked) {
        return topic.optString("name") + " ●";
      } else {
        return topic.optString("name");
      }
    } catch (Exception e) {
      return topic.optString("name");
    }
  }

  private void showTopicDialogOnNewAdded() {
    this.getChannelConfig(channelConfig -> {
      if (channelConfig == null) {
        return;
      }
      JSONArray channelTopics = channelConfig.optJSONArray("channelTopics");
      topicsDialogShowWhenNewAdded = channelConfig.optBoolean("topicsDialogShowWhenNewAdded");
      if (topicsDialogShowWhenNewAdded && hasNewTopicAfterOneHour(channelTopics)) {
        showTopicsDialog();
        updateLastAutoShowedTime();
      } else {
        showPendingTopicsDialog();
      }
    });
  }

  private boolean hasNewTopicAfterOneHour(JSONArray channelTopics) {
    int oneHour = 60 * 60;
    int currentSec = (int) (System.currentTimeMillis() / 1000);
    int secondsAfterLastUpdate = currentSec - getLastAutoShowedTime();
    return isNewTopicAdded(channelTopics) && (getLastAutoShowedTime() == 0 || secondsAfterLastUpdate > oneHour);
  }

  private boolean isNewTopicAdded(JSONArray channelTopics) {
    for (int i = 0; i < channelTopics.length(); i++) {
      JSONObject topic = null;
      try {
        topic = channelTopics.getJSONObject(i);
      } catch (JSONException exception) {
        Logger.e(LOG_TAG, "Error while getting topic at index " + i, exception);
      }
      if (topic != null) {
        int oneHour = 60 * 60;
        int topicLastChecked = getTopicLastChecked();
        try {
          SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault());
          simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
          Date date = simpleDateFormat.parse(topic.optString("createdAt"));
          int topicCreatedAt = (int) (date.getTime() / 1000);
          if (topicCreatedAt + oneHour > topicLastChecked) {
            return true;
          }
        } catch (Exception e) {
          Date date = new Date();
          int topicCreatedAt = (int) (date.getTime() / 1000);
          if (topicCreatedAt + oneHour > topicLastChecked) {
            return true;
          }
        }
      } else {
        Logger.e(LOG_TAG, "isNewTopicAdded: Topic is null at index " + i);
      }
    }
    return false;
  }

  private int getTopicLastChecked() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    return sharedPreferences.getInt(CleverPushPreferences.TOPIC_LAST_CHECKED, 0);
  }

  private int getLastAutoShowedTime() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    return sharedPreferences.getInt(CleverPushPreferences.LAST_TIME_AUTO_SHOWED, 0);
  }

  private void updateLastAutoShowedTime() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    sharedPreferences.edit()
        .putInt(CleverPushPreferences.LAST_TIME_AUTO_SHOWED, (int) (System.currentTimeMillis() / 1000L)).apply();
  }

  public boolean areNotificationsEnabled() {
    try {
      return NotificationManagerCompat.from(CleverPush.context).areNotificationsEnabled();
    } catch (Exception ignored) {
    }
    return true;
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

  public void setAppBannerShownListener(AppBannerShownListener listener) {
    appBannerShownListener = listener;
  }

  public AppBannerShownListener getAppBannerShownListener() {
    return appBannerShownListener;
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

  public void setAutoClearBadge(boolean autoClearBadge) {
    this.autoClearBadge = autoClearBadge;
  }

  public void setAppBannerDraftsEnabled(boolean isShowDraft) {
    this.isShowDraft = isShowDraft;
  }

  public boolean getAutoClearBadge() {
    return this.autoClearBadge;
  }

  public void setIncrementBadge(boolean incrementBadge) {
    this.incrementBadge = incrementBadge;
  }

  public boolean getIncrementBadge() {
    return this.incrementBadge;
  }

  public boolean notificationOpenShouldStartActivity() {
    return !MetaDataUtils.getManifestMetaBoolean(getContext(), "com.cleverpush.notification_open_activity_disabled");
  }

  public void setIgnoreDisabledNotificationPermission(boolean ignore) {
    this.ignoreDisabledNotificationPermission = ignore;
  }

  public void enableDevelopmentMode() {
    Logger.w(LOG_TAG, "CleverPush SDK is running in development mode. Only use this for testing!");
    this.developmentMode = true;
  }

  public boolean isDevelopmentModeEnabled() {
    return this.developmentMode;
  }

  public void disableNightModeAdaption() {
    this.disableNightModeAdaption = true;
  }

  public boolean isNightModeAdaptionDisabled() {
    return this.disableNightModeAdaption;
  }

  public void setKeepTargetingDataOnUnsubscribe(boolean keepData) {
    this.keepTargetingDataOnUnsubscribe = keepData;
  }

  public void clearSubscriptionData() {
    try {
      subscriptionId = null;
      isSessionStartCalled = false;
      SharedPreferences sharedPreferences = getSharedPreferences(getContext());
      SharedPreferences.Editor editor = sharedPreferences.edit();
      editor.remove(CleverPushPreferences.SUBSCRIPTION_ID);
      editor.remove(CleverPushPreferences.SUBSCRIPTION_LAST_SYNC);
      editor.remove(CleverPushPreferences.SUBSCRIPTION_CREATED_AT);
      if (!this.keepTargetingDataOnUnsubscribe) {
        editor.remove(CleverPushPreferences.SUBSCRIPTION_TOPICS);
        editor.remove(CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION);
        editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS);
        editor.remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES);
      }
      editor.apply();
      TriggeredEvent triggeredEvent = new TriggeredEvent(Constants.CLEVERPUSH_APP_BANNER_UNSUBSCRIBE_EVENT, null);
      CleverPush.getInstance(CleverPush.context).getAppBannerModule().triggerEvent(triggeredEvent);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while clearing subscription data. " + e.getMessage(), e);
    }
  }

  public boolean isAppBannersDisabled() {
    appBannersDisabled =
        getSharedPreferences(getContext()).getBoolean(CleverPushPreferences.APP_BANNERS_DISABLED, false);
    return appBannersDisabled;
  }

  private void saveAppBannersDisabled() {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.remove(CleverPushPreferences.APP_BANNERS_DISABLED).apply();
    editor.putBoolean(CleverPushPreferences.APP_BANNERS_DISABLED, appBannersDisabled);
    editor.apply();
  }

  public void enableAppBanners() {
    appBannersDisabled = false;
    this.saveAppBannersDisabled();
    if (appBannerModule == null) {
      return;
    }
    appBannerModule.enableBanners();
  }

  public void disableAppBanners() {
    appBannersDisabled = true;
    this.saveAppBannersDisabled();
    if (appBannerModule == null) {
      return;
    }
    appBannerModule.disableBanners();
  }

  public void setAppBannerTrackingEnabled(boolean trackingEnabled) {
    if (appBannerModule == null) {
      pendingAppBannerTrackingEnabled = trackingEnabled;
      return;
    }
    appBannerModule.setTrackingEnabled(trackingEnabled);
  }

  public boolean isChannelIdChanged(String storedChannelId, String storedSubscriptionId) {
    return storedSubscriptionId != null && storedChannelId != null && !this.channelId.equals(storedChannelId);
  }

  public String getChannelId(Context context) {
    return getSharedPreferences(context).getString(CleverPushPreferences.CHANNEL_ID, null);
  }

  public String getSubscriptionId(Context context) {
    try {
      return getSharedPreferences(context).getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while retrieving subscription ID", e);
      return null;
    }
  }

  public SharedPreferences getSharedPreferences(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  public void addOrUpdateChannelId(Context context, String channelId) {
    getSharedPreferences(context).edit().putString(CleverPushPreferences.CHANNEL_ID, channelId).apply();
  }

  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  public static CleverPush getInstance(Context context) {
    if (context == null) {
      throw new NullPointerException();
    }
    if (instance == null) {
      instance = new CleverPush(context);
    }
    return instance;
  }

  public static CleverPush getInstance(Context context, boolean isNullAllowed) {
    if (!isNullAllowed) {
      if (context == null) {
        throw new NullPointerException();
      }
    }

    if (instance == null) {
      instance = new CleverPush(context);
    }
    return instance;
  }

  public Context getContext() {
    return CleverPush.context;
  }

  public Collection<NotificationOpenedResult> getUnprocessedOpenedNotifications() {
    return unprocessedOpenedNotifications;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public AppBannerModule getAppBannerModule() {
    return AppBannerModule.init(channelId, this.isShowDraft, getSharedPreferences(getContext()),
        getSharedPreferences(getContext()).edit());
  }

  public Activity getCurrentActivity() {
    if (this.customActivity != null) {
      return this.customActivity;
    }

    return ActivityLifecycleListener.currentActivity;
  }

  public Context getCurrentContext() {
    if (getCurrentActivity() != null) {
      return getCurrentActivity();
    } else if (context != null) {
      return context;
    }
    Logger.e(LOG_TAG, "Current context is null.");
    return null;
  }

  public void resetInitSessionCalled() {
    if (this.appBannerModule != null) {
      this.appBannerModule.isInitSessionCalled = false;
    }
  }

  public boolean isPendingInitFeaturesCall() {
    return pendingInitFeaturesCall;
  }

  public boolean isPendingRequestLocationPermissionCall() {
    return pendingRequestLocationPermissionCall;
  }

  public long getSessionStartedTimestamp() {
    return sessionStartedTimestamp;
  }

  public int getSessionVisits() {
    return sessionVisits;
  }

  public SubscribedListener getSubscribedListener() {
    return this.subscribedListener;
  }

  public boolean isSubscriptionInProgress() {
    return this.subscriptionInProgress;
  }

  public boolean isTrackingConsentRequired() {
    return trackingConsentRequired;
  }

  public boolean hasTrackingConsent() {
    return hasTrackingConsent;
  }

  public boolean hasTrackingConsentCalled() {
    return hasTrackingConsentCalled;
  }

  public Collection<TrackingConsentListener> getTrackingConsentListeners() {
    return trackingConsentListeners;
  }

  public boolean isSubscribeConsentRequired() {
    return subscribeConsentRequired;
  }

  public boolean hasSubscribeConsent() {
    return hasSubscribeConsent;
  }

  public boolean hasSubscribeConsentCalled() {
    return hasSubscribeConsentCalled;
  }

  public Collection<SubscribeConsentListener> getSubscribeConsentListeners() {
    return subscribeConsentListeners;
  }

  public List<TriggeredEvent> getPendingAppBannerEvents() {
    return pendingAppBannerEvents;
  }

  public boolean isConfirmAlertShown() {
    return confirmAlertShown;
  }

  public boolean isShowingTopicsDialog() {
    return showingTopicsDialog;
  }

  public Handler getHandler() {
    return new Handler();
  }

  public String getPendingShowAppBannerId() {
    return pendingShowAppBannerId;
  }

  public String getCurrentPageUrl() {
    return currentPageUrl;
  }

  public ArrayList<PageView> getPendingPageViews() {
    return pendingPageViews;
  }

  public static boolean isSubscribeForTopicsDialog() {
    return isSubscribeForTopicsDialog;
  }

  public static void setIsSubscribeForTopicsDialog(boolean isSubscribeForTopicsDialog) {
    CleverPush.isSubscribeForTopicsDialog = isSubscribeForTopicsDialog;
  }

  public static void removeInstance() {
    instance = null;
  }

  public void setInitializeListener(InitializeListener initializeListener) {
    this.initializeListener = initializeListener;
    fireInitializeListener();
  }

  public void fireInitializeListener() {
    if (initializeListener != null) {
      initializeListener.onInitialized();
    }
  }

  public NotificationOpenedListenerBase getNotificationOpenedListener() {
    return notificationOpenedListener;
  }

  public void setNotificationStyle(NotificationStyle style) {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(CleverPushPreferences.NOTIFICATION_STYLE, style.getCode());
    editor.apply();
  }

  public void setUpNotificationCategoryGroups() {
    getChannelConfig(config -> {
      if (config == null) {
        return;
      }
      NotificationCategorySetUp.setNotificationCategoryFromChannelConfig(getContext(), config);
    });
  }

  public ActivityLifecycleListener getActivityLifecycleListener() {
    return ActivityLifecycleListener.getInstance();
  }

  public void getAppBanners(AppBannersListener listener, String channelId) {
    AppBannerModule appBannerModule = getAppBannerModule();
    appBannerModule.getBannerList(listener, channelId);
  }

  public void getAppBannersByGroup(AppBannersListener appBannersListener, String group) {
    if (appBannersListener == null) {
      return;
    }

    AppBannerModule appBannerModule = getAppBannerModule();
    appBannerModule.getBannerListByGroup(appBannersListener, this.channelId, group);
  }

  public boolean isAppOpen() {
    return this.isAppOpen;
  }

  public void setCustomActivity(Activity customActivity) {
    this.customActivity = customActivity;
  }

  public Activity getCustomActivity() {
    return this.customActivity;
  }

  public void setLogListener(LogListener logListener) {
    Logger.setLogListener(logListener);
  }

  public long getNotificationOpenedActivityDestroyedAt() {
    return notificationOpenedActivityDestroyedAt;
  }

  public void setNotificationOpenedActivityDestroyedAt(long notificationOpenedActivityDestroyedAt) {
    this.notificationOpenedActivityDestroyedAt = notificationOpenedActivityDestroyedAt;
  }

  public String getAuthorizerToken() {
    return authorizerToken;
  }

  /**
   * Sets an authorization token for API calls.
   *
   * @param authorizerToken The authorization token to be set.
   */
  public void setAuthorizerToken(String authorizerToken) {
    this.authorizerToken = authorizerToken;
  }

  public boolean isSubscriptionChanged() {
    return isSubscriptionChanged;
  }

  public void setSubscriptionChanged(boolean subscriptionChanged) {
    isSubscriptionChanged = subscriptionChanged;
  }

  /**
   * This method used for TCF2 CMP
   * If get consent 1 in IABTCF_VendorConsents at position 1139 then perform subscribe or tracking according to IabTcfMode
   */
  protected void setTCF() {
    try {
      IabTcfMode mode = getIabTcfMode();
      setTrackingConsentRequired(mode == IabTcfMode.TRACKING_WAIT_FOR_CONSENT);
      setSubscribeConsentRequired(mode == IabTcfMode.SUBSCRIBE_WAIT_FOR_CONSENT);

      Context mContext = context.getApplicationContext();
      SharedPreferences mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

      SharedPreferencesLiveData mSharedPreferencesLiveData = new SharedPreferencesLiveData(mPreferences, IABTCF_VendorConsents);

      mSharedPreferencesLiveData.observeForever(new Observer<String>() {
        @Override
        public void onChanged(String vendorConsents) {
          try {
            if (vendorConsents != null && !vendorConsents.isEmpty()) {
              if (vendorConsents.length() > IABTCF_VendorConsent_POSITION - 1) {
                char consentStatus = vendorConsents.charAt(IABTCF_VendorConsent_POSITION - 1); // charAt uses zero-based indexing, so the 1139th character is at index 1138.
                boolean hasConsent = (consentStatus == '1');
                if (mode == IabTcfMode.TRACKING_WAIT_FOR_CONSENT) {
                  setTrackingConsent(hasConsent);
                }
                if (mode == IabTcfMode.SUBSCRIBE_WAIT_FOR_CONSENT) {
                  setSubscribeConsent(hasConsent);
                }

                if (!hasConsent) {
                  Logger.d(LOG_TAG, "setTCF Vendor does not have consent");
                }
              } else {
                Logger.d(LOG_TAG, "setTCF Vendor consents string is too short to get character at index " + IABTCF_VendorConsent_POSITION + ".");
              }
            }
          } catch (Exception e) {
            Logger.e(LOG_TAG, "Error processing VendorConsents for IABTCF", e);
          }
        }
      });
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in setTCF", e);
    }
  }

  /**
   * Sets the IAB TCF mode for handling tracking or subscription based on consent status.
   * Possible modes: SUBSCRIBE_WAIT_FOR_CONSENT, TRACKING_WAIT_FOR_CONSENT, DISABLED.
   *
   * @param mode The IAB TCF mode to be set.
   */
  public void setIabTcfMode(IabTcfMode mode) {
    this.iabTcfMode = mode;
  }

  protected IabTcfMode getIabTcfMode() {
    return iabTcfMode;
  }

  private int getLocalTrackEventRetentionDays() {
    return trackEventRetentionDays;
  }

  /**
   * Sets the retention period (in days) for local tracking event data.
   * Default is 90 days; data older than this period is deleted from the database.
   *
   * @param trackEventRetentionDays Number of days to retain local tracking event data.
   */
  public void setLocalTrackEventRetentionDays(int trackEventRetentionDays) {
    this.trackEventRetentionDays = trackEventRetentionDays;
  }

  /**
   * Retrieves the current date and time in the format "yyyy-MM-dd HH:mm:ss".
   *
   * @return The current date and time as a formatted string.
   */
  public String getCurrentDateTime() {
    try {
      Date time = Calendar.getInstance().getTime();
      SimpleDateFormat outputFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return outputFmt.format(time);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while getting current date and time", e);
      return "";
    }
  }

  /**
   * Deletes data from the database based on the configured retention days.
   * Retention days are obtained from the result of getLocalTrackEventRetentionDays().
   */
  private void deleteDataBasedOnRetentionDays() {
    try {
      int retentionDays = getLocalTrackEventRetentionDays();
      Logger.d(LOG_TAG, "Retention days: "+ retentionDays);
      DatabaseClient.getInstance(CleverPush.context)
              .getAppDatabase()
              .trackEventDao()
              .deleteDataBasedOnRetentionDays(retentionDays);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while deleting data based on retention days", e);
    }
  }

  private boolean isAutoResubscribe() {
    return autoResubscribe;
  }

  /**
   * Sets the auto-resubscribe flag, indicating whether the application should automatically attempt
   * to resubscribe when notification permission is granted, and the subscriptionId is null.
   *
   * @param autoResubscribe True to enable auto-resubscribe, False to disable it.
   */
  public void setAutoResubscribe(boolean autoResubscribe) {
    this.autoResubscribe = autoResubscribe;
  }

  private boolean shouldAutoRequestNotificationPermission() {
    return autoRequestNotificationPermission;
  }

  /**
   * Sets the autoRequestNotificationPermission flag, determining whether the application should automatically
   * request notification permission when subscribing. If set to false, the notification permission dialog
   * will not be displayed if permission is not given during the subscription process.
   *
   * @param autoRequestNotificationPermission True to automatically request notification permission during subscribe,
   *                                           False to disable automatic notification permission requests.
   */
  public void setAutoRequestNotificationPermission(boolean autoRequestNotificationPermission) {
    this.autoRequestNotificationPermission = autoRequestNotificationPermission;
  }
}
