package com.cleverpush.banner;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.TriggeredEvent;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerDismissType;
import com.cleverpush.banner.models.BannerFrequency;
import com.cleverpush.banner.models.BannerStatus;
import com.cleverpush.banner.models.BannerStopAtType;
import com.cleverpush.banner.models.BannerSubscribedType;
import com.cleverpush.banner.models.BannerTargetEvent;
import com.cleverpush.banner.models.BannerTrigger;
import com.cleverpush.banner.models.BannerTriggerCondition;
import com.cleverpush.banner.models.BannerTriggerConditionEventProperty;
import com.cleverpush.banner.models.BannerTriggerConditionType;
import com.cleverpush.banner.models.BannerTriggerType;
import com.cleverpush.banner.models.CheckFilterRelation;
import com.cleverpush.banner.models.VersionComparison;
import com.cleverpush.database.DatabaseClient;
import com.cleverpush.database.TableBannerTrackEvent;
import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.AppBannersListener;
import com.cleverpush.responsehandlers.SendBannerEventResponseHandler;
import com.cleverpush.util.Logger;
import com.cleverpush.util.PreferenceManagerUtils;
import com.cleverpush.util.VersionComparator;
import com.cleverpush.util.VoucherCodeUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class AppBannerModule {

  private static final String TAG = "CleverPush/AppBanner";
  private static final String APP_BANNER_SHARED_PREFS = "com.cleverpush.appbanner";
  private static final String SHOWN_APP_BANNER_PREF = "shownAppBanners";
  private static final long MIN_SESSION_LENGTH = 30 * 60 * 1000L;
  private static AppBannerModule instance;
  private final boolean showDrafts;
  private final List<TriggeredEvent> events = new ArrayList<>();
  private final HandlerThread handlerThread = new HandlerThread("AppBannerModule");
  private final Handler handler;
  private final SharedPreferences sharedPreferences;
  private final SharedPreferences.Editor editor;
  private String channel;
  private long lastSessionTimestamp;
  private int sessions;
  private boolean loading = false;
  private Collection<AppBannerPopup> filteredBanners = new ArrayList<>();
  private Collection<AppBannerPopup> pendingBanners = new ArrayList<>();
  private LinkedList<AppBannerPopup> pendingFilteredBanners = new LinkedList<>();
  private Collection<Banner> allBanners = null;
  private Collection<AppBannersListener> bannersListeners = new ArrayList<>();
  private boolean trackingEnabled = true;
  HashMap<String, String> currentVoucherCodePlaceholder = new HashMap<>();
  public boolean isInitSessionCalled = false;
  private Collection<String> pendingBannerAPI = new ArrayList<>();
  private String pendingShowAppBannerId = null;
  private String pendingShowAppBannerNotificationId = null;
  private Set<String> bannerClickedList = new HashSet<>();
  private Set<String> bannerDeliveredList = new HashSet<>();
  public String currentEventId = null;
  Set<String> currentNotificationDeeplink = new HashSet<>();

  private AppBannerModule(String channel, boolean showDrafts, SharedPreferences sharedPreferences,
                          SharedPreferences.Editor editor) {
    this.channel = channel;
    this.showDrafts = showDrafts;
    this.sharedPreferences = sharedPreferences;
    this.editor = editor;
    this.sessions = this.getSessions();

    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    editor.putBoolean(CleverPushPreferences.APP_BANNER_SHOWING, false);
    editor.apply();
  }

  public static AppBannerModule init(String channel, SharedPreferences sharedPreferences,
                                     SharedPreferences.Editor editor) {
    return init(channel, false, sharedPreferences, editor);
  }

  public static AppBannerModule init(String channel, boolean showDrafts, SharedPreferences sharedPreferences,
                                     SharedPreferences.Editor editor) {
    if (instance == null) {
      instance = new AppBannerModule(channel, showDrafts, sharedPreferences, editor);
    }
    return instance;
  }

  private View getRoot() {
    return getCurrentActivity().getWindow().getDecorView().getRootView();
  }

  private void loadBanners(String channelId) {
    loadBanners(null, channelId);
  }

  private void loadBanners() {
    loadBanners(null, channel);
  }

  private Comparator<Banner> createBannerComparator() {
    return new Comparator<Banner>() {
      @Override
      public int compare(Banner banner1, Banner banner2) {
        int result;
        if (banner1.getStartAt() == null || banner2.getStartAt() == null) {
          return 0;
        }
        Date date1 = banner1.getStartAt();
        Date date2 = banner2.getStartAt();
        Calendar calendar1 = Calendar.getInstance();
        Calendar calendar2 = Calendar.getInstance();
        calendar1.setTime(date1);
        calendar2.setTime(date2);
        calendar1.set(Calendar.MILLISECOND, 0);
        calendar1.set(Calendar.SECOND, 0);
        calendar2.set(Calendar.MILLISECOND, 0);
        calendar2.set(Calendar.SECOND, 0);

        result = calendar1.compareTo(calendar2);

        if (result == 0) {
          result = banner1.getName().compareTo(banner2.getName());
        }

        return result;
      }
    };
  }

  private Comparator<AppBannerPopup> createAppBannerPopupComparator() {
    return new Comparator<AppBannerPopup>() {
      @Override
      public int compare(AppBannerPopup popup1, AppBannerPopup popup2) {
        Banner banner1 = popup1.getData();
        Banner banner2 = popup2.getData();

        Comparator<Banner> bannerComparator = createBannerComparator();

        return bannerComparator.compare(banner1, banner2);
      }
    };
  }

  void loadBanners(String notificationId, String channelId) {
    if (isLoading()) {
      if (notificationId != null) {
        pendingBannerAPI.add(notificationId);
      }
      return;
    }

    setLoading(true);
    String bannersPath = "/channel/" + channelId + "/app-banners?platformName=Android";
    if (getCleverPushInstance().isDevelopmentModeEnabled()) {
      bannersPath += "&t=" + System.currentTimeMillis();
    }
    if (notificationId != null && !notificationId.isEmpty()) {
      bannersPath += "&notificationId=" + notificationId;
    }
    Logger.d(TAG, "Loading banners: " + bannersPath);
    CleverPushHttpClient.getWithRetry(bannersPath, new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        setLoading(false);
        allBanners = new LinkedList<>();
        try {
          JSONObject responseJson = new JSONObject(response);
          JSONArray rawBanners = responseJson.getJSONArray("banners");

          for (int i = 0; i < rawBanners.length(); ++i) {
            JSONObject rawBanner = rawBanners.getJSONObject(i);
            Banner banner = Banner.create(rawBanner);
            allBanners.add(banner);
          }

          List<Banner> bannerList = new ArrayList<>(allBanners);
          Collections.sort(bannerList, new Comparator<Banner>() {
            @Override
            public int compare(Banner banner1, Banner banner2) {
              int result;
              if (banner1.getStartAt() == null || banner2.getStartAt() == null) {
                return 0;
              }
              Date date1 = banner1.getStartAt();
              Date date2 = banner2.getStartAt();
              Calendar calendar1 = Calendar.getInstance();
              Calendar calendar2 = Calendar.getInstance();
              calendar1.setTime(date1);
              calendar2.setTime(date2);
              calendar1.set(Calendar.MILLISECOND, 0);
              calendar1.set(Calendar.SECOND, 0);
              calendar2.set(Calendar.MILLISECOND, 0);
              calendar2.set(Calendar.SECOND, 0);

              result = calendar2.compareTo(calendar1);

              if (result == 0) {
                result = banner2.getName().compareTo(banner1.getName());
              }
              return result;
            }
          });

          allBanners.clear();
          allBanners.addAll(bannerList);

          for (AppBannersListener listener : getBannersListeners()) {
            listener.ready(allBanners);
          }

          bannersListeners = new ArrayList<>();
        } catch (Exception ex) {
          Logger.e(TAG, "Error in sorting AppBanners." + ex.getMessage(), ex);
        }

        try {
          if (!pendingBannerAPI.isEmpty()) {
            for (String notificationId : pendingBannerAPI) {
              if (notificationId.equalsIgnoreCase(pendingShowAppBannerNotificationId)) {
                showBanner(pendingShowAppBannerId, pendingShowAppBannerNotificationId);
                pendingShowAppBannerId = null;
                pendingShowAppBannerNotificationId = null;
                pendingBannerAPI.clear();
              }
            }
          }
        } catch (Exception e) {
          Logger.e(TAG, "loadBanners error at showing pending banners", e);
        }

        showSilentNotificationBanners();
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        setLoading(false);
        if (throwable != null) {
          Logger.e("CleverPush", "Something went wrong when loading banners." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage(),
                  throwable
          );
        } else {
          Logger.e("CleverPush", "Something went wrong when loading banners." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    });
  }

  /**
   * Displays silent notification banners based on stored preferences.
   * Retrieves silent push banner info from preferences, iterates through entries,
   * and shows corresponding banners using showBanner. Updates preferences after each display.
   */
  private void showSilentNotificationBanners() {
    try {
      String silentPushBanners = sharedPreferences.getString(CleverPushPreferences.SILENT_PUSH_APP_BANNER, null);

      if (silentPushBanners != null) {
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        Map<String, String> silentPushBannersMap = new Gson().fromJson(silentPushBanners, type);

        for (Map.Entry<String, String> entry : silentPushBannersMap.entrySet()) {
          String silentNotificationId = entry.getKey();
          String silentBannerId = entry.getValue();

          showBanner(silentBannerId, silentNotificationId);

          // Remove the key from silentPushBannersMap
          silentPushBannersMap.remove(silentNotificationId);

          // Update preferences with the modified map
          editor.putString(CleverPushPreferences.SILENT_PUSH_APP_BANNER, new Gson().toJson(silentPushBannersMap));
          editor.apply();
        }
      }
    } catch (Exception ignored) {
    }
  }

  void sendBannerEventWithSubscriptionId(String event, Banner banner, String subscriptionId,
                                         String blockId, String screenId, boolean isElementAlreadyClicked, boolean isScreenAlreadyShown) {
    JSONObject jsonBody = getJsonObject();
    try {
      jsonBody.put("bannerId", banner.getId());
      if (banner.getTestId() != null) {
        jsonBody.put("testId", banner.getTestId());
      }
      if (blockId != null && !blockId.isEmpty()) {
        jsonBody.put("blockId", blockId);
      }
      if (screenId != null && !screenId.isEmpty()) {
        jsonBody.put("screenId", screenId);
      }
      jsonBody.put("channelId", channel);
      jsonBody.put("subscriptionId", subscriptionId);

      if (event.equalsIgnoreCase("clicked")) {
        jsonBody.put("isElementAlreadyClicked", isElementAlreadyClicked);
      } else {
        jsonBody.put("isScreenAlreadyShown", isScreenAlreadyShown);
      }
    } catch (JSONException ex) {
      Logger.e(TAG, "Error creating sendBannerEventWithSubscriptionId(/app-banner/event) request parameter.", ex);
    }

    CleverPushHttpClient.post("/app-banner/event/" + event, jsonBody,
        new SendBannerEventResponseHandler().getResponseHandler());
  }

  void sendBannerEvent(String event, Banner banner) {
    sendBannerEvent(event, banner, null, null);
  }

  void sendBannerEvent(String event, Banner banner, String blockId, String screenId) {
    sendBannerEvent(event, banner, blockId, screenId, false, false);
  }

  void sendBannerEvent(String event, Banner banner, String blockId, String screenId, boolean isElementAlreadyClicked, boolean isScreenAlreadyShown) {
    Logger.d(TAG, "sendBannerEvent: " + event);

    if (!this.trackingEnabled) {
      Logger.d(TAG, "sendBannerEvent: not sending event because tracking has been disabled.");
      return;
    }

    if (getCleverPushInstance().isSubscribed()) {
      getCleverPushInstance().getSubscriptionId(subscriptionId -> {
        this.sendBannerEventWithSubscriptionId(event, banner, subscriptionId, blockId, screenId, isElementAlreadyClicked, isScreenAlreadyShown);
      });
    } else {
      Logger.d(TAG, "sendBannerEvent: There is no subscription for CleverPush SDK.");
      this.sendBannerEventWithSubscriptionId(event, banner, null, blockId, screenId, isElementAlreadyClicked, isScreenAlreadyShown);
    }
  }

  public void initSession(String channel) {
    initSession(channel, false);
  }

  public void initSession(String channel, boolean isChannelIdChanged) {
    if (isInitSessionCalled && !isChannelIdChanged) {
      return;
    }
    isInitSessionCalled = true;

    events.clear();

    this.channel = channel;
    if (!getCleverPushInstance().isDevelopmentModeEnabled()
            && lastSessionTimestamp > 0
            && (System.currentTimeMillis() - lastSessionTimestamp) < MIN_SESSION_LENGTH
            && !isChannelIdChanged) {
      return;
    }

    if (getFilteredBanners().size() > 0) {
      for (AppBannerPopup popup : getFilteredBanners()) {
        popup.dismiss();
      }
      filteredBanners = new ArrayList<>();
    }

    lastSessionTimestamp = System.currentTimeMillis();

    sessions += 1;

    this.saveSessions();
    allBanners = null;
    handler.post(this::loadBanners);

    this.startup();
  }

  public int getSessions() {
    return sharedPreferences.getInt(CleverPushPreferences.APP_BANNER_SESSIONS, 0);
  }

  void saveSessions() {
    editor.putInt(CleverPushPreferences.APP_BANNER_SESSIONS, sessions);
    editor.apply();
  }

  public void triggerEvent(TriggeredEvent event) {
    events.add(event);
    currentEventId = event.getId();
    this.startup();
  }

  public void getBannerList(AppBannersListener listener, String channelId) {
    if (listener == null) {
      return;
    }
    if (channelId != null) {
      bannersListeners.add(listener);
      getHandler().post(() -> {
        this.loadBanners(channelId);
      });
    }
  }

  private AppBannersListener createFilteringAppBannerListener(String group, AppBannersListener listener) {
    return (Collection<Banner> loadedBanners) -> {
      LinkedList<Banner> filteredBanners = new LinkedList<>();

      for (Banner banner : loadedBanners) {
        if (banner.getGroup() == null || !banner.getGroup().equals(group) || banner.getStatus() == BannerStatus.Draft) {
          continue;
        }

        if (!isBannerTimeAllowed(banner)) {
          continue;
        }

        if (!isBannerTargetingAllowed(banner)) {
          continue;
        }

        filteredBanners.add(banner);
      }

      listener.ready(filteredBanners);
    };
  }

  public void getBannerListByGroup(AppBannersListener listener, String channelId, String group) {
    if (listener == null) {
      return;
    }

    AppBannersListener filteringListener = createFilteringAppBannerListener(group, listener);
    getBannerList(filteringListener, channelId);
  }

  public void getBanners(AppBannersListener listener) {
    this.getBanners(listener, null);
  }

  public void getBanners(AppBannersListener listener, String notificationId) {
    if (listener == null) {
      return;
    }
    if (notificationId != null) {
      getHandler().post(() -> {
        this.loadBanners(notificationId, channel);
        bannersListeners.add(listener);
      });
    } else {
      if (getListOfBanners() == null) {
        bannersListeners.add(listener);
      } else {
        listener.ready(getListOfBanners());
      }
    }
  }

  void startup() {
    Logger.d(TAG, "startup");

    this.getBanners(banners -> {
      createBanners(banners);
      scheduleFilteredBanners();
    });
  }

  boolean isBannerTimeAllowed(Banner banner) {
    Date now = new Date();
    if (banner == null) {
      return false;
    }
    return banner.getStopAtType() != BannerStopAtType.SpecificTime
        || banner.getStopAt() == null
        || banner.getStopAt().after(now);
  }

  boolean isBannerTargetingAllowed(Banner banner) {
    if (banner == null || this.getCurrentActivity() == null) {
      return false;
    }

    boolean allowed = true;

    if (banner.getLanguages() != null && banner.getLanguages().size() > 0 && !banner.getLanguages()
        .contains(Locale.getDefault().getLanguage())) {
      allowed = false;
    }

    if (banner.getSubscribedType() == BannerSubscribedType.Subscribed && !getCleverPushInstance().isSubscribed()) {
      allowed = false;
    }

    if (banner.getSubscribedType() == BannerSubscribedType.Unsubscribed && getCleverPushInstance().isSubscribed()) {
      allowed = false;
    }

    if (banner.getTags() != null && banner.getTags().size() > 0) {
      allowed = false;

      for (String tag : banner.getTags()) {
        if (getCleverPushInstance().hasSubscriptionTag(tag)) {
          allowed = true;
          break;
        }
      }
    }

    if (allowed && banner.getExcludeTags() != null && banner.getExcludeTags().size() > 0) {
      for (String tag : banner.getExcludeTags()) {
        if (getCleverPushInstance().hasSubscriptionTag(tag)) {
          allowed = false;
          break;
        }
      }
    }

    if (allowed && banner.getTopics() != null && banner.getTopics().size() > 0) {
      allowed = false;

      for (String topic : banner.getTopics()) {
        if (getCleverPushInstance().hasSubscriptionTopic(topic)) {
          allowed = true;
          break;
        }
      }
    }

    if (allowed && banner.getExcludeTopics() != null && banner.getExcludeTopics().size() > 0) {
      for (String topic : banner.getExcludeTopics()) {
        if (getCleverPushInstance().hasSubscriptionTopic(topic)) {
          allowed = false;
          break;
        }
      }
    }

    if (allowed && banner.getAttributes() != null && banner.getAttributes().size() > 0) {
      if (!getCleverPushInstance().isSubscribed()) {
        return false;
      }

      for (HashMap<String, String> attribute : banner.getAttributes()) {
        String attributeId = attribute.get("id");
        String compareAttributeValue = attribute.get("value") != null ? attribute.get("value") : "";
        String fromValue = attribute.get("fromValue") != null ? attribute.get("fromValue") : "";
        String toValue = attribute.get("toValue") != null ? attribute.get("toValue") : "";
        String relationString = attribute.get("relation");
        if (relationString == null) {
          relationString = "equals";
        }
        String attributeValue = (String) getCleverPushInstance().getSubscriptionAttribute(attributeId);
        if (attributeValue == null) {
          return false;
        }

        if (!this.checkRelationFilter(allowed, CheckFilterRelation.fromString(relationString), attributeValue,
            compareAttributeValue, fromValue, toValue)) {
          allowed = false;
          break;
        }
      }
    }

    if (allowed) {
      allowed = appVersionFilter(true, banner);
    }

    return allowed;
  }

  /**
   * App Banner Version Filter
   */
  private boolean appVersionFilter(boolean allowed, Banner banner) {
    try {
      PackageInfo pInfo =
          this.getCurrentActivity().getPackageManager().getPackageInfo(this.getCurrentActivity().getPackageName(), 0);
      String versionName = pInfo.versionName;
      return this.checkAppVersionRelationFilter(allowed, banner.getBannerAppVersionFilterRelation(), versionName,
          banner.getAppVersionFilterValue(), banner.getFromVersion(), banner.getToVersion());
    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner checking app version filter", e);
    }

    return allowed;
  }

  private boolean checkRelationFilter(boolean allowed, CheckFilterRelation relation, String compareValue,
                                      String attributeValue, String fromValue, String toValue) {
    if (relation == null) {
      return allowed;
    }
    try {
      if (allowed && relation.equals(CheckFilterRelation.Equals)) {
        if (!compareValue.equals(attributeValue)) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.NotEqual)) {
        if (compareValue.equals(attributeValue)) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.GreaterThan)) {
        if (Double.parseDouble(compareValue) < (Double.parseDouble(attributeValue))) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.LessThan)) {
        if (Double.parseDouble(compareValue) > (Double.parseDouble(attributeValue))) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.Contains)) {
        if (!compareValue.contains(attributeValue)) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.NotContains)) {
        if (compareValue.contains(attributeValue)) {
          allowed = false;
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner checking relation filter", e);
    }

    return allowed;
  }

  private boolean checkAppVersionRelationFilter(boolean allowed, CheckFilterRelation relation, String appVersion,
                                                String compareValue, String fromVersion, String toVersion) {
    VersionComparator vc = new VersionComparator();

    VersionComparison result = vc.compare(appVersion, compareValue);
    VersionComparison resultFrom = vc.compare(appVersion, fromVersion);
    VersionComparison resultTo = vc.compare(appVersion, toVersion);

    if (relation == null) {
      return allowed;
    }

    try {
      if (allowed && relation.equals(CheckFilterRelation.Equals)) {
        if (result != VersionComparison.EQUALS) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.Between)) {
        if (resultFrom != VersionComparison.EQUALS
            && resultFrom != VersionComparison.GREATER_THAN
            && resultTo != VersionComparison.EQUALS
            && resultTo != VersionComparison.GREATER_THAN) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.NotEqual)) {
        if (result == VersionComparison.EQUALS) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.GreaterThan)) {
        if (result != VersionComparison.GREATER_THAN) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.LessThan)) {
        if (result != VersionComparison.LESS_THAN) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.Contains)) {
        if (!appVersion.contains(compareValue)) {
          allowed = false;
        }
      }

      if (allowed && relation.equals(CheckFilterRelation.NotContains)) {
        if (appVersion.contains(compareValue)) {
          allowed = false;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      Logger.e(TAG, "Error in AppBanner checking app version filter", e);
    }

    return allowed;
  }

  private boolean checkEventTriggerCondition(BannerTriggerCondition condition) {
    for (TriggeredEvent triggeredEvent : events) {
      if (triggeredEvent.getId() == null || !triggeredEvent.getId().equals(condition.getEvent())) {
        continue;
      }

      if (condition.getEventProperties() == null || condition.getEventProperties().size() == 0) {
        return true;
      }

      boolean conditionTrue = true;

      for (BannerTriggerConditionEventProperty eventProperty : condition.getEventProperties()) {
        String propertyValue = String.valueOf(triggeredEvent.getProperties().get(eventProperty.getProperty()));
        String comparePropertyValue = eventProperty.getValue();

        boolean eventPropertiesMatching = this.checkRelationFilter(true,
            CheckFilterRelation.fromString(eventProperty.getRelation()),
            propertyValue,
            comparePropertyValue,
            comparePropertyValue,
            comparePropertyValue);

        if (!eventPropertiesMatching) {
          conditionTrue = false;
          break;
        }
      }

      if (conditionTrue) {
        return true;
      }
    }

    return false;
  }

  private boolean checkTargetEventRelationFilter(CheckFilterRelation relation, String compareValue,
                                                 String attributeValue, String fromValue, String toValue, String bannerId, String event, List<BannerTriggerConditionEventProperty> eventProperties) {
    try {
      if (events.isEmpty() || event == null || event.isEmpty() || compareValue == null || compareValue.isEmpty()) {
        return false;
      }

      for (TriggeredEvent triggeredEvent : events) {
        if (triggeredEvent.getId() == null || !triggeredEvent.getId().equals(event)) {
          continue;
        }

        if (eventProperties.size() > 0) {
          if (!checkEventProperties(eventProperties, event)) {
            return false;
          }

          for (BannerTriggerConditionEventProperty eventProperty : eventProperties) {
            List<TableBannerTrackEvent> bannerTrackEvents = DatabaseClient.getInstance(CleverPush.context)
                .getAppDatabase()
                .trackEventDao()
                .getBannerTrackEvent(bannerId, event, compareValue, attributeValue, String.valueOf(relation), fromValue, toValue, eventProperty.getRelation(), eventProperty.getProperty(), eventProperty.getValue());

            if (bannerTrackEvents.isEmpty()) {
              return false;
            }

            for (TableBannerTrackEvent bannerTrackEvent : bannerTrackEvents) {
              int count = bannerTrackEvent.getCount();
              String createdDate = bannerTrackEvent.getCreatedDateTime();
              int pastDays = getDaysDifference(createdDate);

              if (pastDays > Integer.parseInt(bannerTrackEvent.getProperty())) {
                return false;
              }

              if (!checkRelation(count, relation, attributeValue, fromValue, toValue)) {
                return false;
              }
            }
          }
          return true;
        }

        List<TableBannerTrackEvent> bannerTrackEvents = DatabaseClient.getInstance(CleverPush.context)
            .getAppDatabase()
            .trackEventDao()
            .getBannerTrackEvent(bannerId, event, compareValue, attributeValue, String.valueOf(relation), fromValue, toValue, "", "", "");

        if (bannerTrackEvents.isEmpty()) {
          return false;
        }

        for (TableBannerTrackEvent bannerTrackEvent : bannerTrackEvents) {
          int count = bannerTrackEvent.getCount();
          String property = bannerTrackEvent.getProperty();
          String createdDate = bannerTrackEvent.getCreatedDateTime();
          int pastDays = getDaysDifference(createdDate);

          if (pastDays > Integer.parseInt(property)) {
            continue;
          }

          if (!checkRelation(count, relation, attributeValue, fromValue, toValue)) {
            continue;
          }

          return true;
        }
        return false;
      }
    } catch (Exception e) {
      Logger.e(TAG, "checkTargetEventRelationFilter: Error in AppBanner checking target event relation filter.", e);
    }
    return false;
  }

  private boolean checkEventProperties(List<BannerTriggerConditionEventProperty> eventProperties, String event) {
    for (BannerTriggerConditionEventProperty eventProperty : eventProperties) {
      boolean propertyMatched = false;
      for (TriggeredEvent triggeredEvent : events) {
        if (triggeredEvent.getId() == null || !triggeredEvent.getId().equals(event) || triggeredEvent.getProperties() == null) {
          continue;
        }
        String propertyValue = String.valueOf(triggeredEvent.getProperties().get(eventProperty.getProperty()));
        String comparePropertyValue = eventProperty.getValue();

        boolean eventPropertiesMatching = checkRelationFilterForTargetEvent(true,
            CheckFilterRelation.fromString(eventProperty.getRelation()),
            propertyValue,
            comparePropertyValue,
            comparePropertyValue,
            comparePropertyValue);

        if (eventPropertiesMatching) {
          propertyMatched = true;
          break;  // Break inner loop if a match is found
        }
      }
      if (!propertyMatched) {
        return false;  // Return false if no matching event is found for the current eventProperty
      }
    }
    return true;  // Return true only if all properties match
  }

  private boolean checkRelationFilterForTargetEvent(boolean allowed, CheckFilterRelation relation, String compareValue,
                                                    String attributeValue, String fromValue, String toValue) {
    if (relation == null) {
      return false;
    }
    try {
      switch (relation) {
        case Equals:
          return compareValue.equals(attributeValue);
        case NotEqual:
          return !compareValue.equals(attributeValue);
        case GreaterThan:
          return Double.parseDouble(compareValue) > Double.parseDouble(attributeValue);
        case LessThan:
          return Double.parseDouble(compareValue) < Double.parseDouble(attributeValue);
        case Contains:
          return compareValue.contains(attributeValue);
        case NotContains:
          return !compareValue.contains(attributeValue);
        default:
          return false;
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner checking relation filter for target event", e);
      return false;
    }
  }

  private boolean checkRelation(int count, CheckFilterRelation relation, String attributeValue, String fromValue, String toValue) {
    switch (relation) {
      case Equals:
        return count == Integer.parseInt(attributeValue);
      case NotEqual:
        return count != Integer.parseInt(attributeValue);
      case GreaterThan:
        return count > Integer.parseInt(attributeValue);
      case LessThan:
        return count < Integer.parseInt(attributeValue);
      case Between:
        int from = Integer.parseInt(fromValue);
        int to = Integer.parseInt(toValue);
        return count >= from && count <= to;
      default:
        return false;
    }
  }

  private boolean checkDeeplinkTriggerCondition(BannerTriggerCondition condition) {
    Set<String> deepLinks = getCurrentNotificationDeeplink();
    Logger.e(TAG, "deepLinks: " + new Gson().toJson(deepLinks));
    if (deepLinks.size() == 0) {
      return false;
    }

    for (String deepLink : deepLinks) {
      if (deepLink.equalsIgnoreCase(condition.getDeepLinkUrl())) {
        return true;
      }
    }
    return false;
  }

  private void createBanners(Collection<Banner> banners) {
    for (Banner banner : banners) {
      if (banner.getFrequency() == BannerFrequency.Once && isBannerShown(banner.getId())) {
        Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Frequency");
        continue;
      }

      boolean triggers = false, isTriggerCondition = false, isTargetEvent = false;
      if (banner.getTriggerType() == BannerTriggerType.Conditions) {
        isTriggerCondition = true;
        for (BannerTrigger trigger : banner.getTriggers()) {
          boolean triggerTrue = true;
          for (BannerTriggerCondition condition : trigger.getConditions()) {
            // true by default to make the AND check work
            boolean conditionTrue = true;
            if (condition.getType() != null) {
              if (condition.getType().equals(BannerTriggerConditionType.Duration)) {
                banner.setDelaySeconds(condition.getSeconds());
              } else if (condition.getType().equals(BannerTriggerConditionType.Sessions)) {
                if (condition.getRelation().equals("lt")) {
                  conditionTrue = sessions < condition.getSessions();
                } else {
                  conditionTrue = sessions > condition.getSessions();
                }
              } else if (condition.getType().equals(BannerTriggerConditionType.Event) && condition.getEvent() != null) {
                conditionTrue = this.checkEventTriggerCondition(condition);
              } else if (condition.getType().equals(BannerTriggerConditionType.Deeplink)) {
                conditionTrue = this.checkDeeplinkTriggerCondition(condition);
              } else {
                conditionTrue = false;
              }
            }
            if (!conditionTrue) {
              triggerTrue = false;
              break;
            }
          }
          if (triggerTrue) {
            triggers = true;
            break;
          }
        }
      }

      boolean targetEvents = true;
      if (banner.getEventFilters() != null && banner.getEventFilters().size() > 0) {
        isTargetEvent = true;
        for (BannerTargetEvent bannerTargetEvent : banner.getEventFilters()) {
          boolean targetConditionTrue;
          String event = bannerTargetEvent.getEvent();
          String property = bannerTargetEvent.getProperty();
          String relationString = bannerTargetEvent.getRelation();
          String value = bannerTargetEvent.getValue();
          String fromValue = bannerTargetEvent.getFromValue();
          String toValue = bannerTargetEvent.getToValue();
          List<BannerTriggerConditionEventProperty> eventProperties = bannerTargetEvent.getEventProperties();
          if (!isValidTargetValues(event, property, relationString, value, fromValue, toValue, banner.getId())) {
            continue;
          }

          String relation = String.valueOf(CheckFilterRelation.fromString(relationString));

          if (eventProperties.size() > 0) {
            for (BannerTriggerConditionEventProperty eventProperty : eventProperties) {
              handleBannerTrackEvent(banner.getId(), event, property, value, relation, fromValue, toValue,
                  eventProperty.getRelation(), eventProperty.getProperty(), eventProperty.getValue());
            }
          } else {
            handleBannerTrackEvent(banner.getId(), event, property, value, relation, fromValue, toValue, "", "", "");
          }

          targetConditionTrue = this.checkTargetEventRelationFilter(CheckFilterRelation.fromString(relationString)
              , property
              , value != null ? value : ""
              , fromValue != null ? fromValue : ""
              , toValue != null ? toValue : ""
              , banner.getId()
              , event
              , eventProperties);

          if (!targetConditionTrue) {
            targetEvents = false;
            break;
          }
        }
      }

      if (isTriggerCondition && isTargetEvent) {
        if (!targetEvents || !triggers) {
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Trigger and Target Event not satisfied " + sessions);
          continue;
        }
      } else if (isTriggerCondition) {
        if (!triggers) {
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Trigger not satisfied " + sessions);
          continue;
        }
      } else if (isTargetEvent) {
        if (!targetEvents) {
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Target Event not satisfied " + sessions);
          continue;
        }
      }

      boolean contains = false;
      for (AppBannerPopup popup : filteredBanners) {
        if (popup.getData().getId().equals(banner.getId())) {
          contains = true;
          break;
        }
      }

      if (!contains) {
        getActivityLifecycleListener().setActivityInitializedListener(
            () -> filteredBanners.add(new AppBannerPopup(getCurrentActivity(), banner)));
      }
    }
  }

  private void handleBannerTrackEvent(String bannerId, String event, String property, String value, String relation,
                                      String fromValue, String toValue, String eventPropertyRelation,
                                      String eventProperty, String eventPropertyValue) {
    ArrayList<TableBannerTrackEvent> bannerTrackEvents = (ArrayList<TableBannerTrackEvent>) DatabaseClient.getInstance(CleverPush.context)
        .getAppDatabase()
        .trackEventDao()
        .getBannerTrackEvent(bannerId, event, property, value != null ? value : "", relation,
            fromValue != null ? fromValue : "", toValue != null ? toValue : "",
            eventPropertyRelation != null ? eventPropertyRelation : "",
            eventProperty != null ? eventProperty : "",
            eventPropertyValue != null ? eventPropertyValue : "");

    if (bannerTrackEvents.size() == 0) {
      TableBannerTrackEvent bannerTrackEvent = new TableBannerTrackEvent();
      bannerTrackEvent.setBannerId(bannerId);
      bannerTrackEvent.setEventId(event);
      bannerTrackEvent.setRelation(relation);
      bannerTrackEvent.setProperty(property);
      bannerTrackEvent.setValue(value != null ? value : "");
      bannerTrackEvent.setFromValue(fromValue != null ? fromValue : "");
      bannerTrackEvent.setToValue(toValue != null ? toValue : "");
      bannerTrackEvent.setCount(0);
      bannerTrackEvent.setCreatedDateTime(getCleverPushInstance().getCurrentDateTime());
      bannerTrackEvent.setUpdatedDateTime(getCleverPushInstance().getCurrentDateTime());
      bannerTrackEvent.setEventPropertyRelation(eventPropertyRelation != null ? eventPropertyRelation : "");
      bannerTrackEvent.setEventProperty(eventProperty != null ? eventProperty : "");
      bannerTrackEvent.setEventPropertyValue(eventPropertyValue != null ? eventPropertyValue : "");

      DatabaseClient.getInstance(CleverPush.context)
          .getAppDatabase()
          .trackEventDao()
          .insert(bannerTrackEvent);
    }
  }

  void scheduleFilteredBanners() {
    if (getCleverPushInstance().isAppBannersDisabled()) {
      pendingBanners.addAll(getFilteredBanners());
      getFilteredBanners().removeAll(pendingBanners);
      return;
    }

    Date now = new Date();
    for (AppBannerPopup bannerPopup : getFilteredBanners()) {
      Banner banner = bannerPopup.getData();

      boolean isEveryTrigger = banner.getFrequency() == BannerFrequency.Every_Trigger && banner.getTriggerType() == BannerTriggerType.Conditions;

      // banner frequency is every trigger then we allow banner display multiple times
      if (banner.isScheduled() && !isEveryTrigger) {
        continue;
      }

      if (!checkIsEveryTrigger(banner, isEveryTrigger)) {
        continue;
      }

      banner.setScheduled();

      if (banner.getStartAt().before(now)) {
        if (banner.getDelaySeconds() > 0) {
          getHandler().postDelayed(() -> showBanner(bannerPopup), 1000L * banner.getDelaySeconds());
        } else {
          getHandler().post(() -> showBanner(bannerPopup));
        }
      } else {
        long delay = banner.getStartAt().getTime() - now.getTime();
        getHandler().postDelayed(() -> showBanner(bannerPopup), delay + (1000L * banner.getDelaySeconds()));
      }
    }

    if (currentEventId != null) {
      currentEventId = null;
    }
  }

  private boolean isValidTargetValues(String event, String property, String relationString, String value, String fromValue, String toValue, String bannerId) {
    if (event == null || event.isEmpty()) {
      Logger.d(TAG, "Skipping Target in banner " + bannerId + "because: track event is not valid");
      return false;
    }
    if (!isValidIntegerValue(property)) {
      Logger.d(TAG, "Skipping Target in banner " + bannerId + "because: property value is not valid");
      return false;
    }
    if (relationString == null || relationString.isEmpty()) {
      Logger.d(TAG, "Skipping Target in banner " + bannerId + "because: relation is not valid");
      return false;
    }
    if (relationString.equalsIgnoreCase("between")) {
      if (!isValidIntegerValue(fromValue)) {
        Logger.d(TAG, "Skipping Target in banner " + bannerId + "because: from value is not valid");
        return false;
      }
      if (!isValidIntegerValue(toValue)) {
        Logger.d(TAG, "Skipping Target in banner " + bannerId + "because: to value is not valid");
        return false;
      }
    } else {
      if (!isValidIntegerValue(value)) {
        Logger.d(TAG, "Skipping Target in banner " + bannerId + "because: value is not valid");
        return false;
      }
    }
    return true;
  }
  /**
   * currently we only accept whole numbers here since the amount of how often an event can be triggered can not be a floating number or negative number
   */
  private boolean isValidIntegerValue(String value) {
    if (value == null || value.isEmpty() || Double.parseDouble(value) < 0 || Double.parseDouble(value) % 1 != 0) {
      return false;
    }
    return true;
  }

  private boolean checkIsEveryTrigger(Banner banner, boolean isEveryTrigger) {
    String bannerEventId = null;
    if (!isEveryTrigger || currentEventId == null || banner.getTriggerType() != BannerTriggerType.Conditions) {
      return true;
    }
    for (BannerTrigger trigger : banner.getTriggers()) {
      for (BannerTriggerCondition condition : trigger.getConditions()) {
        if (condition.getType() == null
                || !condition.getType().equals(BannerTriggerConditionType.Event)
                || condition.getEvent() == null) {
          continue;
        }
        bannerEventId = condition.getEvent();
        if (currentEventId.equalsIgnoreCase(bannerEventId)) {
          return true;
        }
      }
    }

    if (bannerEventId != null && !bannerEventId.equalsIgnoreCase(currentEventId)) {
      return false;
    }
    return true;
  }

  public void showBanner(String bannerId, String notificationId) {
    showBanner(bannerId, notificationId, false);
  }

  public void showBanner(String bannerId, String notificationId, boolean force) {
    getActivityLifecycleListener().setActivityInitializedListener(new ActivityInitializedListener() {
      @Override
      public void initialized() {
        Logger.d(TAG, "showBannerById: " + bannerId);
        if (bannerId != null && notificationId != null) {
          pendingShowAppBannerId = bannerId;
          pendingShowAppBannerNotificationId = notificationId;
        }
        getBanners(banners -> {
          for (Banner banner : banners) {
            if (banner.getId().equals(bannerId)) {
              AppBannerPopup popup = getAppBannerPopup(banner);

              if (getCleverPushInstance().isAppBannersDisabled()) {
                pendingBanners.add(popup);
                break;
              }
              getHandler().post(() -> showBanner(popup, force));
              break;
            }
          }
        }, notificationId);
      }
    });
  }

  public AppBannerPopup getAppBannerPopup(Banner banner) {
    return new AppBannerPopup(getCurrentActivity(), banner);
  }

  void showBanner(AppBannerPopup bannerPopup, boolean force) {
    try {
      Banner banner = bannerPopup.getData();
      if (!force) {
        if (sharedPreferences.getBoolean(CleverPushPreferences.APP_BANNER_SHOWING, false)) {
          pendingFilteredBanners.add(bannerPopup);
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: A Banner is already on the screen");
          return;
        }

        if (banner.getStatus() == BannerStatus.Draft && !showDrafts) {
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Draft");
          return;
        }

        if (!isBannerTimeAllowed(banner)) {
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Stop Time");
          return;
        }

        if (!isBannerTargetingAllowed(banner)) {
          Logger.d(TAG, "Skipping Banner " + banner.getId() + " because: Targeting not allowed");
          return;
        }
      }

      bannerPopup.init();
      bannerPopup.show();

      getActivityLifecycleListener().setActivityInitializedListener(new ActivityInitializedListener() {
        @Override
        public void initialized() {
          setBannerIsShown(bannerPopup.getData());
        }
      });

      if (bannerPopup.getData().getDismissType() == BannerDismissType.Timeout) {
        long timeout = Math.max(0, bannerPopup.getData().getDismissTimeout());
        getHandler().postDelayed(bannerPopup::dismiss, timeout * 1000);
      }

      bannerPopup.setOpenedListener(action -> {
        if (getCleverPushInstance().getAppBannerOpenedListener() != null) {
          getCleverPushInstance().getAppBannerOpenedListener().opened(action);
        }

        if (action.getType().equals("subscribe")) {
          getCleverPushInstance().subscribe();
        }

        if (action.getType().equals("addTags")) {
          List<String> tags = action.getTags();
          if (tags != null) {
            getCleverPushInstance().addSubscriptionTags(action.getTags().toArray(new String[0]));
          }
        }

        if (action.getType().equals("removeTags")) {
          List<String> tags = action.getTags();
          if (tags != null) {
            getCleverPushInstance().removeSubscriptionTags(action.getTags().toArray(new String[0]));
          }
        }

        if (action.getType().equals("addTopics")) {
          List<String> topicsList = action.getTopics();
          if (topicsList != null) {
            Set<String> topics = getCleverPushInstance().getSubscriptionTopics();
            topics.addAll(action.getTopics());
            getCleverPushInstance().setSubscriptionTopics(topics.toArray(new String[0]));
          }
        }

        if (action.getType().equals("removeTopics")) {
          List<String> topicsList = action.getTopics();
          if (topicsList != null) {
            Set<String> topics = getCleverPushInstance().getSubscriptionTopics();
            topics.removeAll(action.getTopics());
            getCleverPushInstance().setSubscriptionTopics(topics.toArray(new String[0]));
          }
        }

        if (action.getType().equals("setAttribute")) {
          getCleverPushInstance().setSubscriptionAttribute(action.getAttributeId(), action.getAttributeValue());
        }

        if (action.getType().equals("copyToClipboard")) {
          String copyText = action.getName();
          if (copyText.contains("{voucherCode}")) {
            String voucherCode = "";
            HashMap<String, String> currentVoucherCodePlaceholder = CleverPush.getInstance(CleverPush.context).getAppBannerModule().getCurrentVoucherCodePlaceholder();
            if (currentVoucherCodePlaceholder != null && currentVoucherCodePlaceholder.containsKey(banner.getId())) {
              voucherCode = currentVoucherCodePlaceholder.get(banner.getId());
            }
            copyText = VoucherCodeUtils.replaceVoucherCodeString(copyText, voucherCode);
          }
          ClipboardManager clipboard = (ClipboardManager) CleverPush.context.getSystemService(Context.CLIPBOARD_SERVICE);
          ClipData clip = ClipData.newPlainText("Voucher Code", copyText);
          clipboard.setPrimaryClip(clip);
        }

        if (action.getType().equals("geoLocation")) {
          getCleverPushInstance().requestLocationPermission();
        }
      });

      PreferenceManagerUtils.updateSharedPreferenceByKey(CleverPush.context, CleverPushPreferences.APP_BANNER_SHOWING, true);

      if (getCleverPushInstance().getAppBannerShownListener() != null) {
        getCleverPushInstance().getAppBannerShownListener().shown(banner);
      }
    } catch (Exception ex) {
      Logger.e(TAG, "Error in showBanner. " + ex.getMessage());
    }
  }

  void showBanner(AppBannerPopup bannerPopup) {
    try {
      showBanner(bannerPopup, false);
    } catch (Exception ex) {
      Logger.e(TAG, ex.getMessage(), ex);
    }
  }

  private boolean isBannerShown(String id) {
    if (getCurrentActivity() == null) {
      return false;
    }

    SharedPreferences sharedPreferences =
        getCurrentActivity().getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
    Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

    if (shownBanners == null) {
      return false;
    }

    return shownBanners.contains(id);
  }

  void setBannerIsShown(Banner banner) {
    if (getCurrentActivity() == null) {
      return;
    }

    SharedPreferences sharedPreferences =
        getCurrentActivity().getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
    Set<String> shownBanners = new HashSet<>(sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>()));

    assert shownBanners != null;
    shownBanners.add(banner.getId());

    if (banner.getConnectedBanners() != null) {
      for (String connectedBannerId : banner.getConnectedBanners()) {
        if (shownBanners.contains(connectedBannerId)) {
          continue;
        }
        shownBanners.add(connectedBannerId);
      }
    }

    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.remove(SHOWN_APP_BANNER_PREF);
    editor.apply();
    editor.putStringSet(SHOWN_APP_BANNER_PREF, shownBanners);
    editor.apply();
  }

  public void enableBanners() {
    if (getPendingBanners() != null && getPendingBanners().size() > 0) {
      filteredBanners.addAll(getPendingBanners());
      getPendingBanners().clear();
      this.scheduleFilteredBanners();
    }
  }

  public void disableBanners() {
    pendingBanners = new ArrayList<>();
  }

  public void setTrackingEnabled(boolean trackingEnabled) {
    this.trackingEnabled = trackingEnabled;
  }

  @Override
  protected void finalize() throws Throwable {
    handlerThread.quit();
    super.finalize();
  }

  public long getLastSessionTimestamp() {
    return lastSessionTimestamp;
  }

  public boolean isLoading() {
    return loading;
  }

  public void setLoading(boolean loading) {
    this.loading = loading;
  }

  public Collection<Banner> getListOfBanners() {
    return allBanners;
  }

  public Collection<AppBannerPopup> getFilteredBanners() {
    List<AppBannerPopup> appBannerPopupList = new ArrayList<>(filteredBanners);
    Collections.sort(appBannerPopupList, createAppBannerPopupComparator());

    filteredBanners.clear();
    for (AppBannerPopup appBannerPopup: appBannerPopupList) {
      filteredBanners.add(appBannerPopup);
    }

    return filteredBanners;
  }

  public CleverPush getCleverPushInstance() {
    return CleverPush.getInstance(CleverPush.context, true);
  }

  public Collection<AppBannersListener> getBannersListeners() {
    return bannersListeners;
  }

  public void clearBannersListeners() {
    bannersListeners.clear();
  }

  public JSONObject getJsonObject() {
    return new JSONObject();
  }

  public Handler getHandler() {
    return handler;
  }

  public Collection<AppBannerPopup> getPendingBanners() {
    return pendingBanners;
  }

  void showPendingFilteredBanners() {
    if (pendingFilteredBanners.size() == 0) {
      return;
    }

    int size = pendingFilteredBanners.size();
    for (int i = 0; i < size; i++) {
      AppBannerPopup bannerPopup = pendingFilteredBanners.get(0);
      pendingFilteredBanners.remove(0);
      getHandler().post(() -> showBanner(bannerPopup));
    }
  }

  public void onAppBannerDismiss() {
    showPendingFilteredBanners();
  }

  public void clearPendingBanners() {
    pendingBanners.clear();
  }

  public Activity getCurrentActivity() {
    return CleverPush.getInstance(CleverPush.context).getCurrentActivity();
  }

  public ActivityLifecycleListener getActivityLifecycleListener() {
    return ActivityLifecycleListener.getInstance();
  }

  public String getChannel() {
    return channel;
  }

  public HashMap<String, String> getCurrentVoucherCodePlaceholder() {
    return currentVoucherCodePlaceholder;
  }

  public void setCurrentVoucherCodePlaceholder(HashMap<String, String> currentVoucherCodePlaceholder) {
    this.currentVoucherCodePlaceholder = currentVoucherCodePlaceholder;
  }

  protected boolean isBannerElementClicked(String id) {
    if (bannerClickedList == null) {
      return false;
    }

    return bannerClickedList.contains(id);
  }

  void setIsBannerElementClicked(String id) {
    if (bannerClickedList != null) {
      bannerClickedList.add(id);
    }
  }

  public boolean isBannerScreenDelivered(String id) {
    if (bannerDeliveredList == null) {
      return false;
    }

    return bannerDeliveredList.contains(id);
  }

  void setIsBannerScreenDelivered(String id) {
    if (bannerDeliveredList != null) {
      bannerDeliveredList.add(id);
    }
  }

  public void clearBannerTrackList() {
    bannerClickedList.clear();
    bannerDeliveredList.clear();
  }

  public int getDaysDifference(String createdDate) {
    try {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
      Date dateCreated = sdf.parse(createdDate);
      String currentDateTimeString = getCleverPushInstance().getCurrentDateTime();
      Date currentDate = sdf.parse(currentDateTimeString);
      Calendar createdCalendar = Calendar.getInstance();
      createdCalendar.setTime(dateCreated);
      Calendar currentCalendar = Calendar.getInstance();
      currentCalendar.setTime(currentDate);
      createdCalendar.set(Calendar.HOUR_OF_DAY, 0);
      createdCalendar.set(Calendar.MINUTE, 0);
      createdCalendar.set(Calendar.SECOND, 0);
      currentCalendar.set(Calendar.HOUR_OF_DAY, 0);
      currentCalendar.set(Calendar.MINUTE, 0);
      currentCalendar.set(Calendar.SECOND, 0);
      long timeDifference = currentCalendar.getTimeInMillis() - createdCalendar.getTimeInMillis();
      int daysDifference = (int) (timeDifference / (1000 * 60 * 60 * 24));
      return daysDifference;
    } catch (Exception e) {
      Logger.e(TAG, "Error in getting days difference for target event relation filter.", e);
      return -1;
    }
  }

  public Set<String> getCurrentNotificationDeeplink() {
    return currentNotificationDeeplink;
  }

  public void setCurrentNotificationDeeplink(Set<String> currentNotificationDeeplink) {
    this.currentNotificationDeeplink = currentNotificationDeeplink;
  }
}
