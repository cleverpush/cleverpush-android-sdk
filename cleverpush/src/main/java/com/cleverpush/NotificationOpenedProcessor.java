package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.core.app.NotificationManagerCompat;

import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;
import com.google.gson.Gson;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class NotificationOpenedProcessor {

  public static void processIntent(Context context, Intent intent) {
    try {
      CleverPush.setAppContext(context);

      Gson gson = new Gson();
      Notification notification = gson.fromJson(intent.getStringExtra("notification"), Notification.class);
      Subscription subscription = gson.fromJson(intent.getStringExtra("subscription"), Subscription.class);
      String actionIndex = intent.getStringExtra("actionIndex");
      int activeNotificationId = intent.getIntExtra("notificationId", 0);

      if (notification == null || subscription == null) {
        return;
      }

      // Close the notification using NotificationManager
      NotificationManagerCompat.from(context).cancel(notification.getTag(), activeNotificationId);

      String notificationId = notification.getId();
      String subscriptionId = subscription.getId();

      if (notificationId == null || subscriptionId == null) {
        return;
      }

      NotificationOpenedResult result = new NotificationOpenedResult();
      result.setNotification(notification);
      result.setSubscription(subscription);
      result.setNotificationOpenedActivity((Activity) context);

      CleverPush cleverPush = CleverPush.getInstance(context);
      String channelId = cleverPush.getChannelId(context);

      cleverPush.trackNotificationClicked(notificationId, subscriptionId, channelId, actionIndex);

      cleverPush.fireNotificationOpenedListener(result);

      if (notification.getAppBanner() != null && notification.getAppBanner().length() > 0
          && notification.getVoucherCode() != null && notification.getVoucherCode().length() > 0) {
        HashMap<String, String> currentVoucherCodePlaceholder = new HashMap<>();

        if (cleverPush.getAppBannerModule().getCurrentVoucherCodePlaceholder() != null) {
          currentVoucherCodePlaceholder = cleverPush.getAppBannerModule().getCurrentVoucherCodePlaceholder();
        }

        currentVoucherCodePlaceholder.put(notification.getAppBanner(), notification.getVoucherCode());
        cleverPush.getAppBannerModule().setCurrentVoucherCodePlaceholder(currentVoucherCodePlaceholder);
      }

      // open launcher activity
      boolean shouldStartActivity = cleverPush.notificationOpenShouldStartActivity();
      Logger.d(LOG_TAG, "NotificationOpenedProcessor shouldStartActivity: " + shouldStartActivity);

      boolean appInBackground = false, appInForeground = false, customNotificationActivityEnabled = false;
      customNotificationActivityEnabled = cleverPush.isCustomNotificationActivityEnabled();
      if (ActivityLifecycleListener.getInstance() != null) {
        appInBackground = ActivityLifecycleListener.getInstance().isAppInBackground();
        appInForeground = ActivityLifecycleListener.getInstance().isAppOpen();
      }

      if (shouldStartActivity) {
        if (!customNotificationActivityEnabled) {
          Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
          if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launchIntent);
          }
        } else if (!appInForeground && !appInBackground) {
          Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
          if (launchIntent != null) {
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            context.startActivity(launchIntent);
          }
        }
      }

      String notificationDeeplinkId = null;
      String notificationUrl = notification.getUrl();

      if (notificationUrl != null && !notificationUrl.isEmpty()) {
        try {
          Uri uri = Uri.parse(notificationUrl);
          notificationDeeplinkId = uri.getQueryParameter("deeplinkId");
        } catch (Exception e) {
          Logger.e("CleverPush", "Error parsing deeplinkId from notification URL: " + notificationUrl, e);
        }
      }

      if (notificationDeeplinkId != null && !notificationDeeplinkId.isEmpty()) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(CleverPushPreferences.LAST_CLICKED_NOTIFICATION_DEEPLINK_ID, notificationDeeplinkId);
        editor.putString(CleverPushPreferences.LAST_CLICKED_NOTIFICATION_DEEPLINK_TIME, cleverPush.getCurrentDateTime());
        editor.apply();
      }

      try {
        boolean autoHandleDeepLink = notification.isAutoHandleDeepLink();
        String deepLinkURL = result.getNotification().getUrl();
        if (autoHandleDeepLink && deepLinkURL != null && !deepLinkURL.isEmpty()) {
          setNotificationDeepLink(deepLinkURL, cleverPush);
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(deepLinkURL));
          context.startActivity(browserIntent);
        }
      } catch (Exception e) {
        Logger.e(LOG_TAG, "Error while handling auto handle deep link for notification id: " + notificationId, e);
      }

      boolean badgeEnabled = notification.getCategory() == null || !notification.getCategory().getBadgeDisabled();
      if (badgeEnabled) {
        BadgeHelper.update(context, cleverPush.getIncrementBadge());
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while processing intent for push: " + e.getMessage(), e);
    }
  }

  public static void setNotificationDeepLink(String deepLink, CleverPush cleverPush) {
    try {
      URI uri = new URI(deepLink);

      // Remove query parameters
      uri = new URI(uri.getScheme(), uri.getAuthority(), uri.getPath(), null, uri.getFragment());

      Set<String> currentDeeplink = new HashSet<>();

      if (cleverPush.getAppBannerModule().getCurrentNotificationDeeplink() != null) {
        currentDeeplink = cleverPush.getAppBannerModule().getCurrentNotificationDeeplink();
      }

      currentDeeplink.add(uri.toString());
      cleverPush.getAppBannerModule().setCurrentNotificationDeeplink(currentDeeplink);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while setting notification deep link: " + e.getMessage(), e);
    }
  }

  public static SharedPreferences getSharedPreferences(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(SharedPreferencesManager.SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);
    return sharedPreferences;
  }

}
