package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.app.NotificationManagerCompat;

import com.cleverpush.util.Logger;
import com.google.gson.Gson;

import java.util.HashMap;

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

      if (shouldStartActivity) {
        Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (launchIntent != null) {
          launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
          context.startActivity(launchIntent);
        }
      }

      try {
        boolean autoHandleDeepLink = notification.isAutoHandleDeepLink();
        if (autoHandleDeepLink && result.getNotification().getUrl() != null && !result.getNotification().getUrl().isEmpty()) {
          Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getNotification().getUrl()));
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
}
