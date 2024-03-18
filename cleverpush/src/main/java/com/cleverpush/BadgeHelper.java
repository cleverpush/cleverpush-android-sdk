package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.cleverpush.shortcutbadger.ShortcutBadgeException;
import com.cleverpush.shortcutbadger.ShortcutBadger;
import com.cleverpush.util.Logger;

public class BadgeHelper {
  public static void update(Context context, boolean incrementBadge) {
    update(context, incrementBadge, 0);
  }

  public static void update(Context context, boolean incrementBadge, int additionalCount) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      updateBadge(context, incrementBadge, additionalCount);
    } else {
      // we will only support badges for > M for now
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  static boolean isGroupSummary(StatusBarNotification notif) {
    return (notif.getNotification().flags & NotificationCompat.FLAG_GROUP_SUMMARY) != 0;
  }

  static NotificationManager getNotificationManager(Context context) {
    return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  public static StatusBarNotification[] getActiveNotifications(Context context) {
    StatusBarNotification[] statusBarNotifications = new StatusBarNotification[] {};
    try {
      statusBarNotifications = getNotificationManager(context).getActiveNotifications();
    } catch (Throwable e) {
      Logger.e(LOG_TAG, "Error getting active notifications", e);
    }
    return statusBarNotifications;
  }

  @RequiresApi(api = Build.VERSION_CODES.M)
  private static void updateBadge(Context context, boolean incrementBadge, int additionalCount) {
    StatusBarNotification[] activeNotifications = getActiveNotifications(context);

    int runningCount = additionalCount;
    for (StatusBarNotification activeNotification : activeNotifications) {
      if (isGroupSummary(activeNotification)) {
        continue;
      }
      runningCount++;
    }

    SharedPreferences sharedPreferences = CleverPush.getInstance(context).getSharedPreferences(context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putInt(CleverPushPreferences.NOTIFICATION_BADGE_COUNT, runningCount);
    editor.apply();

    if (!incrementBadge && runningCount > 0) {
      runningCount = 1;
    }

    updateCount(runningCount, context);
  }

  static void updateCount(int count, Context context) {
    try {
      ShortcutBadger.applyCountOrThrow(context, count);
    } catch (ShortcutBadgeException e) {
      Logger.e(LOG_TAG, "Error updating badge count", e);
    }
  }
}
