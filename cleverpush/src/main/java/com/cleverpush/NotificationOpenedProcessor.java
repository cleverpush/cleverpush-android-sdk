package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.Intent;
import com.cleverpush.util.Logger;

import com.google.gson.Gson;

public class NotificationOpenedProcessor {

    public static void processIntent(Context context, Intent intent) {
        CleverPush.setAppContext(context);

        Gson gson = new Gson();
        Notification notification = gson.fromJson(intent.getStringExtra("notification"), Notification.class);
        Subscription subscription = gson.fromJson(intent.getStringExtra("subscription"), Subscription.class);

        if (notification == null || subscription == null) {
            return;
        }

        String notificationId = notification.getId();
        String subscriptionId = subscription.getId();

        if (notificationId == null || subscriptionId == null) {
            return;
        }

        NotificationOpenedResult result = new NotificationOpenedResult();
        result.setNotification(notification);
        result.setSubscription(subscription);

        CleverPush cleverPush = CleverPush.getInstance(context);

        cleverPush.trackNotificationClicked(notificationId, subscriptionId);
        cleverPush.fireNotificationOpenedListener(result);

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

        boolean badgeEnabled = notification.getCategory() == null || !notification.getCategory().getBadgeDisabled();
        if (badgeEnabled) {
            BadgeHelper.update(context, cleverPush.getIncrementBadge());
        }
    }
}
