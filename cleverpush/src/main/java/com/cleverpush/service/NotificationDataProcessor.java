package com.cleverpush.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.BadgeHelper;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.Subscription;

import java.util.HashSet;
import java.util.Set;

import static com.cleverpush.service.NotificationExtenderService.EXTENDER_SERVICE_JOB_ID;

public class NotificationDataProcessor {
    public static void process(Context context, Notification notification, Subscription subscription) {
        if (notification == null || subscription == null) {
            return;
        }

        String notificationId = notification.getId();
        String subscriptionId = subscription.getId();

        if (notificationId == null || subscriptionId == null) {
            return;
        }

        CleverPush cleverPush = CleverPush.getInstance(context);

        boolean dontShowNotification = false;

        // default behaviour: do not show notification if application is in the foreground
		// ways to bypass this:
		// - use NotificationReceivedCallbackListener and return false
		// - use NotificationExtenderService (here you can also modify the NotificationBuilder)
        try {
            boolean callbackReceivedListener = cleverPush.isNotificationReceivedListenerCallback();

            NotificationOpenedResult result = new NotificationOpenedResult();
            result.setNotification(notification);
            result.setSubscription(subscription);

            if (callbackReceivedListener) {
                dontShowNotification = !cleverPush.fireNotificationReceivedCallbackListener(result);
            } else {
                if (NotificationService.getInstance().applicationInForeground(context)) {
                    dontShowNotification = cleverPush.fireNotificationReceivedListener(result);
                } else {
                    cleverPush.fireNotificationReceivedListener(result);
                }
            }

        } catch (Exception e) {
            Log.e("CleverPush", "Error checking if application is in foreground", e);
        }

		// do not show silent notifications
		if (notification.isSilent()) {
			dontShowNotification = true;
		}

        boolean hasExtenderService = startExtenderService(context, notification, subscription);
        if (hasExtenderService) {
        	dontShowNotification = true;
		}

        if (!dontShowNotification) {
			NotificationService.getInstance().showNotification(context, notification, subscription);
        }

        cleverPush.trackNotificationDelivered(notificationId, subscriptionId);

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            Set<String> notifications = sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>());
            if (notifications != null) {
                notifications.add(notification.getRawPayload());
            }
            editor.remove(CleverPushPreferences.NOTIFICATIONS).apply();
            editor.putStringSet(CleverPushPreferences.NOTIFICATIONS, notifications);
            editor.putString(CleverPushPreferences.LAST_NOTIFICATION_ID, notificationId);
            editor.commit();
        } catch (Exception e) {
            Log.e("CleverPush", "Error saving notification to shared preferences", e);
        }

		BadgeHelper.update(context, cleverPush.getIncrementBadge(), 1);
    }

    private static boolean startExtenderService(Context context, Notification notification, Subscription subscription) {
		Intent intent = NotificationExtenderService.getIntent(context);
		if (intent == null) {
			return false;
		}

		intent.putExtra("notification", notification);
		intent.putExtra("subscription", subscription);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			NotificationExtenderService.enqueueWork(
				context,
				intent.getComponent(),
				EXTENDER_SERVICE_JOB_ID,
				intent
			);
		} else {
			context.startService(intent);
		}

		return true;
	}
}
