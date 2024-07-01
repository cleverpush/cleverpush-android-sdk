package com.cleverpush.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPushPreferences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SharedPreferencesManager {

  public static final String SDK_PREFERENCES_NAME = "CleverPush";
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;
  private Context context;

  public SharedPreferencesManager(Context context) {
    this.context = context;
    sharedPreferences = context.getSharedPreferences(SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);
    editor = sharedPreferences.edit();
  }

  public static SharedPreferences getSharedPreferences(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(SharedPreferencesManager.SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);
    return sharedPreferences;
  }

  /**
   * Migrate default shared preferences to CleverPush shared preferences
   * This method copies all key-value pairs from the default shared preferences to CleverPush shared preferences.
   */
  public static void migrateSharedPreferences(Context context) {
    try {
      SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
      SharedPreferences sdkPreferences = context.getSharedPreferences(SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);

      // Define the keys to be migrated
      Map<String, String> keysToMigrate = new HashMap<>();
      keysToMigrate.put("fcmToken", CleverPushPreferences.FCM_TOKEN);
      keysToMigrate.put("hmsToken", CleverPushPreferences.HMS_TOKEN);
      keysToMigrate.put("admToken", CleverPushPreferences.ADM_TOKEN);
      keysToMigrate.put("channelId", CleverPushPreferences.CHANNEL_ID);
      keysToMigrate.put("subscriptionId", CleverPushPreferences.SUBSCRIPTION_ID);
      keysToMigrate.put("subscriptionLastSync", CleverPushPreferences.SUBSCRIPTION_LAST_SYNC);
      keysToMigrate.put("subscriptionTags", CleverPushPreferences.SUBSCRIPTION_TAGS);
      keysToMigrate.put("subscriptionTopics", CleverPushPreferences.SUBSCRIPTION_TOPICS);
      keysToMigrate.put("subscriptionTopicsVersion", CleverPushPreferences.SUBSCRIPTION_TOPICS_VERSION);
      keysToMigrate.put("subscriptionAttributes", CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES);
      keysToMigrate.put("subscriptionLanguage", CleverPushPreferences.SUBSCRIPTION_LANGUAGE);
      keysToMigrate.put("subscriptionCountry", CleverPushPreferences.SUBSCRIPTION_COUNTRY);
      keysToMigrate.put("subscriptionCreatedAt", CleverPushPreferences.SUBSCRIPTION_CREATED_AT);
      keysToMigrate.put("notifications", CleverPushPreferences.NOTIFICATIONS);
      keysToMigrate.put("notificationsJson", CleverPushPreferences.NOTIFICATIONS_JSON);
      keysToMigrate.put("lastNotificationId", CleverPushPreferences.LAST_NOTIFICATION_ID);
      keysToMigrate.put("lastClickedNotificationId", CleverPushPreferences.LAST_CLICKED_NOTIFICATION_ID);
      keysToMigrate.put("lastClickedNotificationTime", CleverPushPreferences.LAST_CLICKED_NOTIFICATION_TIME);
      keysToMigrate.put("appOpens", CleverPushPreferences.APP_OPENS);
      keysToMigrate.put("appReviewShownAt", CleverPushPreferences.APP_REVIEW_SHOWN);
      keysToMigrate.put("pendingTopicsDialog", CleverPushPreferences.PENDING_TOPICS_DIALOG);
      keysToMigrate.put("appBannerSessions", CleverPushPreferences.APP_BANNER_SESSIONS);
      keysToMigrate.put("appBannersDisabled", CleverPushPreferences.APP_BANNERS_DISABLED);
      keysToMigrate.put("subscriptionTopicsDeselectAll", CleverPushPreferences.SUBSCRIPTION_TOPICS_DESELECT_ALL);
      keysToMigrate.put("openedStories", CleverPushPreferences.APP_OPENED_STORIES);
      keysToMigrate.put("appBannerShowing", CleverPushPreferences.APP_BANNER_SHOWING);
      keysToMigrate.put("unsubscribed", CleverPushPreferences.UNSUBSCRIBED);
      keysToMigrate.put("topicLastChecked", CleverPushPreferences.TOPIC_LAST_CHECKED);
      keysToMigrate.put("lastTimeAutoShowed", CleverPushPreferences.LAST_TIME_AUTO_SHOWED);
      keysToMigrate.put("notificationStyle", CleverPushPreferences.NOTIFICATION_STYLE);
      keysToMigrate.put("deviceId", CleverPushPreferences.DEVICE_ID);
      keysToMigrate.put("silentPushBanners", CleverPushPreferences.SILENT_PUSH_APP_BANNER);

      // Start migration
      SharedPreferences.Editor editor = sdkPreferences.edit();
      for (Map.Entry<String, String> entry : keysToMigrate.entrySet()) {
        String oldKey = entry.getKey();
        String newKey = entry.getValue();

        if (defaultPreferences.contains(oldKey)) {
          Object value = defaultPreferences.getAll().get(oldKey);

          // Handle different data types
          if (value instanceof String) {
            editor.putString(newKey, (String) value);
          } else if (value instanceof Boolean) {
            editor.putBoolean(newKey, (Boolean) value);
          } else if (value instanceof Integer) {
            editor.putInt(newKey, (Integer) value);
          } else if (value instanceof Float) {
            editor.putFloat(newKey, (Float) value);
          } else if (value instanceof Long) {
            editor.putLong(newKey, (Long) value);
          } else if (value instanceof Set<?>) {
            Set<String> stringSet = (Set<String>) value;
            editor.putStringSet(newKey, stringSet);
          }
        }
      }

      Map<String, ?> allEntries = defaultPreferences.getAll();
      for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        if (key.contains("cleverpush")) {
          // Handle different data types
          if (value instanceof String) {
            editor.putString(key, (String) value);
          } else if (value instanceof Boolean) {
            editor.putBoolean(key, (Boolean) value);
          } else if (value instanceof Integer) {
            editor.putInt(key, (Integer) value);
          } else if (value instanceof Float) {
            editor.putFloat(key, (Float) value);
          } else if (value instanceof Long) {
            editor.putLong(key, (Long) value);
          } else if (value instanceof Set<?>) {
            Set<String> stringSet = (Set<String>) value;
            editor.putStringSet(key, stringSet);
          }
        }
      }
      editor.apply();
    } catch (Exception e) {
      Logger.e("CleverPush", "Error while migrating default preference into CleverPush preference. " + e.getLocalizedMessage(), e);
    }
  }

  public void setString(String key, String value) {
    editor.putString(key, value);
    editor.apply();
  }

  public String getString(String key, String defaultValue) {
    return sharedPreferences.getString(key, defaultValue);
  }

  public void setInt(String key, int value) {
    editor.putInt(key, value);
    editor.apply();
  }

  public int getInt(String key, int defaultValue) {
    return sharedPreferences.getInt(key, defaultValue);
  }

  public void setBoolean(String key, boolean value) {
    editor.putBoolean(key, value);
    editor.apply();
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return sharedPreferences.getBoolean(key, defaultValue);
  }

}
