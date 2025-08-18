package com.cleverpush.util;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationManagerCompat;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.NotificationCategory;
import com.cleverpush.NotificationCategoryGroup;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationCategorySetUp {

  public synchronized static void setNotificationCategory(Context context, ArrayList<NotificationCategory> notificationCategories) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }

    Set<String> topicsToAdd = new HashSet<>();
    Set<String> topicsToRemove = new HashSet<>();

    Set<String> subscribedTopicIds = CleverPush.getInstance(context).getSubscriptionTopics();
    if (subscribedTopicIds == null) {
      subscribedTopicIds = new HashSet<>();
    } else {
      subscribedTopicIds = new HashSet<>(subscribedTopicIds);
    }

    for (int i = 0; i < notificationCategories.size(); i++) {
      NotificationCategory category = notificationCategories.get(i);

      if (category.getId() == null || category.getId().isEmpty() || category.getName() == null || category.getName()
          .isEmpty()) {
        continue;
      }

      String updatedAt = category.getUpdatedAt();
      String categoryId = category.getId();

      if (updatedAt != null && !updatedAt.isEmpty()) {
        SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        String notificationChannelUpdatedAt = sharedPreferences.getString(CleverPushPreferences.NOTIFICATION_CHANNEL_UPDATED_AT, null);
        Map<String, String> notificationChannelUpdatedAtMap;

        if (notificationChannelUpdatedAt != null) {
          Type type = new TypeToken<Map<String, String>>() {
          }.getType();
          notificationChannelUpdatedAtMap = new Gson().fromJson(notificationChannelUpdatedAt, type);
        } else {
          notificationChannelUpdatedAtMap = new HashMap<>();
        }

        String existingUpdatedAt = notificationChannelUpdatedAtMap.get(category.getId());

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        long updatedAtMillis = 0;
        long existingUpdatedAtMillis = 0;

        try {
          if (existingUpdatedAt != null && !existingUpdatedAt.isEmpty()) {
            Date updatedDate = isoFormat.parse(updatedAt);
            updatedAtMillis = updatedDate != null ? updatedDate.getTime() : 0;

            Date existingDate = isoFormat.parse(existingUpdatedAt);
            existingUpdatedAtMillis = existingDate != null ? existingDate.getTime() : 0;
          }
        } catch (Exception e) {
          e.printStackTrace();
        }

        if (existingUpdatedAt == null || updatedAtMillis > existingUpdatedAtMillis) {
          deleteNotificationChannelIfExists(context, category.getId(), category.getName());
          notificationChannelUpdatedAtMap.put(category.getId(), updatedAt);
          editor.putString(CleverPushPreferences.NOTIFICATION_CHANNEL_UPDATED_AT, new Gson().toJson(notificationChannelUpdatedAtMap));
          editor.apply();
        }
      }

      NotificationChannel channel
              = new NotificationChannel(geCategoryChannelId(categoryId, updatedAt), category.getName(), NotificationManager.IMPORTANCE_DEFAULT);

      String description = category.getDescription();
      if (description != null) {
        channel.setDescription(description);
      }

      try {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        if (category.getSoundFilename() != null && !category.getSoundFilename().isEmpty()) {
          Resources resources = context.getResources();
          String packageName = context.getPackageName();
          int soundId = resources.getIdentifier(category.getSoundFilename(), "raw", packageName);
          if (soundId != 0) {
            Uri trySoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + soundId);
            if (trySoundUri != null) {
              soundUri = trySoundUri;
            }
          }
        }

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build();
        channel.setSound(soundUri, audioAttributes);
      } catch (Exception e) {
        Logger.e("CleverPush", "Error while setting custom sound for push. " + e.getLocalizedMessage(), e);
      }

      String importance = category.getImportance();
      if (importance != null) {
        if (importance.equalsIgnoreCase("URGENT")) {
          channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        } else if (importance.equalsIgnoreCase("HIGH")) {
          channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
        } else if (importance.equalsIgnoreCase("MEDIUM")) {
          channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
        } else if (importance.equalsIgnoreCase("LOW")) {
          channel.setImportance(NotificationManager.IMPORTANCE_LOW);
        }
      }

      String lockScreen = category.getLockScreen();
      if (lockScreen != null) {
        if (lockScreen.equalsIgnoreCase("PUBLIC")) {
          channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
        } else if (lockScreen.equalsIgnoreCase("PRIVATE")) {
          channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
        } else if (lockScreen.equalsIgnoreCase("SECRET")) {
          channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
        }
      }

      String ledColor = category.getLedColor();
      if (category.getLedColorEnabled() && ledColor != null && !ledColor.equalsIgnoreCase("")) {
        int parsedLedColor = parseColor(ledColor);
        if (parsedLedColor > 0) {
          channel.setLightColor(parsedLedColor);
        }
      }

      channel.setShowBadge(!category.getBadgeDisabled());

      NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

      NotificationCategoryGroup categoryGroup = category.getGroup();
      if (categoryGroup != null) {
        NotificationChannelGroup group = new NotificationChannelGroup(categoryGroup.getId(), categoryGroup.getName());
        notificationManager.createNotificationChannelGroup(group);
        channel.setGroup(group.getId());
      }

      notificationManager.createNotificationChannel(channel);

      processCategoryTopics(context, geCategoryChannelId(category.getId(), category.getUpdatedAt()),
              category.getId(), topicsToAdd, topicsToRemove, category.getTopics());
    }

    updateSubscriptionTopics(context, subscribedTopicIds, topicsToAdd, topicsToRemove);
  }

  public synchronized static void setNotificationCategoryFromChannelConfig(Context context, JSONObject channelConfig) {
    try {
      ArrayList<NotificationCategory> notificationCategories = new ArrayList<>();
      JSONArray notificationCategoryGroups = channelConfig.getJSONArray("notificationCategoryGroups");
      for (int i = 0; i < notificationCategoryGroups.length(); i++) {
        JSONObject notificationCategoryGroupJSONObject = notificationCategoryGroups.getJSONObject(i);
        NotificationCategoryGroup notificationCategoryGroup = new NotificationCategoryGroup();
        notificationCategoryGroup.setId(notificationCategoryGroupJSONObject.optString("_id"));
        notificationCategoryGroup.setName(notificationCategoryGroupJSONObject.optString("name"));
        JSONArray categories = notificationCategoryGroupJSONObject.getJSONArray("categories");
        for (int j = 0; j < categories.length(); j++) {
          JSONObject notificationCategoryJSONObject = categories.getJSONObject(j);

          NotificationCategory notificationCategory = new NotificationCategory();

          notificationCategory.setId(notificationCategoryJSONObject.optString("_id"));
          notificationCategory.setGroup(notificationCategoryGroup);
          notificationCategory.setName(notificationCategoryJSONObject.optString("name"));
          notificationCategory.setDescription(notificationCategoryJSONObject.optString("description"));
          notificationCategory.setSoundEnabled(notificationCategoryJSONObject.optBoolean("soundEnabled"));
          notificationCategory.setSoundFilename(notificationCategoryJSONObject.optString("soundFilename"));
          notificationCategory.setVibrationEnabled(notificationCategoryJSONObject.optBoolean("vibrationEnabled"));
          notificationCategory.setVibrationPattern(notificationCategoryJSONObject.optString("vibrationPattern"));
          notificationCategory.setLedColorEnabled(notificationCategoryJSONObject.optBoolean("ledColorEnabled"));
          notificationCategory.setLedColor(notificationCategoryJSONObject.optString("ledColor"));
          notificationCategory.setLockScreen(notificationCategoryJSONObject.optString("lockScreen"));
          notificationCategory.setImportance(notificationCategoryJSONObject.optString("importance"));
          notificationCategory.setBadgeDisabled(notificationCategoryJSONObject.optBoolean("badgeDisabled"));
          notificationCategory.setBackgroundColor(notificationCategoryJSONObject.optString("backgroundColor"));
          notificationCategory.setForegroundColor(notificationCategoryJSONObject.optString("foregroundColor"));
          notificationCategory.setUpdatedAt(notificationCategoryJSONObject.optString("updatedAt"));

          JSONArray topicsArray = notificationCategoryJSONObject.optJSONArray("topics");
          if (topicsArray != null) {
            List<String> topics = new ArrayList<>();
            for (int k = 0; k < topicsArray.length(); k++) {
              topics.add(topicsArray.optString(k));
            }
            notificationCategory.setTopics(topics);
          }

          notificationCategories.add(notificationCategory);
        }
      }
      setNotificationCategory(context, notificationCategories);
    } catch (JSONException e) {
      Logger.e("NotificationUtils", "Error parsing channel configuration for notification category", e);
    }
  }

  public static int parseColor(String hexStr) {
    if (hexStr == null) {
      return 0;
    }

    if (hexStr.startsWith("rgb(")) {
      Pattern c = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
      Matcher m = c.matcher(hexStr);
      if (m.matches()) {
        hexStr = String.format("#%02x%02x%02x", Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)),
            Integer.parseInt(m.group(3)));
      }
    }

    if (!hexStr.startsWith("#")) {
      hexStr = "#" + hexStr;
    }

    return Color.parseColor(hexStr);
  }

  public static void deleteNotificationChannelIfExists(Context context, String channelId, String channelName) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
      NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      NotificationChannel existingChannel = notificationManager.getNotificationChannel(channelId);

      if (existingChannel != null && existingChannel.getName().equals(channelName)) {
        notificationManager.deleteNotificationChannel(channelId);
      }
    }
  }

  private static String geCategoryChannelId(String categoryId, String updatedAt) {
    String channelId =  categoryId;
    try {
      if (updatedAt != null && !updatedAt.isEmpty()) {
        String sanitizedUpdatedAt = updatedAt.replaceAll("[^a-zA-Z0-9]", "_");
        channelId = categoryId + "_" + sanitizedUpdatedAt;
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while getting category channelId. " + e.getMessage(), e);
    }
    return channelId;
  }

  private static void processCategoryTopics(Context context, String channelId, String channelName,
                                            Set<String> topicsToAdd, Set<String> topicsToRemove, List<String> topics) {
    try {
        if (topics.size() > 0) {
          boolean isChannelEnabled = isNotificationChannelEnabled(context, channelId);
          if (isChannelEnabled) {
            topicsToAdd.addAll(topics);
          } else {
            Logger.i("CleverPush", "Notification channel '" + channelName + "' disabled by the user.");
            topicsToRemove.addAll(topics);
          }
        }
    } catch (Exception e) {
      Logger.e("CleverPush", "NotificationCategorySetUp: Error in processCategoryTopics: " + e.getMessage(), e);
    }
  }

  private synchronized static boolean isNotificationChannelEnabled(Context context, String channelId) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (channelId != null && !channelId.isEmpty()) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
          NotificationChannel channel = manager.getNotificationChannel(channelId);
          return channel != null && channel.getImportance() != NotificationManager.IMPORTANCE_NONE;
        }
      }
      return false;
    } else {
      return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }
  }

  private static void updateSubscriptionTopics(Context context, Set<String> subscribedTopicIds,
                                               Set<String> topicsToAdd, Set<String> topicsToRemove) {
    try {
      boolean changed = false;

      for (String topicId : topicsToAdd) {
        if (!subscribedTopicIds.contains(topicId)) {
          subscribedTopicIds.add(topicId);
          changed = true;
        }
      }
      for (String topicId : topicsToRemove) {
        if (subscribedTopicIds.contains(topicId)) {
          subscribedTopicIds.remove(topicId);
          changed = true;
        }
      }

      if (changed) {
        CleverPush.getInstance(context)
                .setSubscriptionTopics(subscribedTopicIds.toArray(new String[0]));
      }
    } catch (Exception e) {
      Logger.e("CleverPush", "NotificationCategorySetUp: Error updating subscription topics: " + e.getMessage(), e);
    }
  }

}
