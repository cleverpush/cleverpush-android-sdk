package com.cleverpush.util;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import com.cleverpush.NotificationCategory;
import com.cleverpush.NotificationCategoryGroup;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationCategorySetUp {

  public static void setNotificationCategory(Context context, ArrayList<NotificationCategory> notificationCategories) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
      return;
    }

    for (int i = 0; i < notificationCategories.size(); i++) {
      NotificationCategory category = notificationCategories.get(i);

      if (category.getId() == null || category.getId().isEmpty() || category.getName() == null || category.getName()
          .isEmpty()) {
        continue;
      }

      NotificationChannel channel =
          new NotificationChannel(category.getId(), category.getName(), NotificationManager.IMPORTANCE_DEFAULT);

      String description = category.getDescription();
      if (description != null) {
        channel.setDescription(description);
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
    }
  }

  public static void setNotificationCategoryFromChannelConfig(Context context, JSONObject channelConfig) {
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
}
