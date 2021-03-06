package com.cleverpush.service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.Subscription;
import com.cleverpush.util.LimitedSizeQueue;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

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

        cleverPush.trackNotificationDelivered(notificationId, subscriptionId);

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

        try {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
            SharedPreferences.Editor editor = sharedPreferences.edit();

			Gson gson = new Gson();
            int maximumNotifications = 100;
            String notificationsJson = sharedPreferences.getString(CleverPushPreferences.NOTIFICATIONS_JSON, null);
			Type type = new TypeToken<List<Notification>>() {}.getType();

			LimitedSizeQueue<Notification> notifications = null;
            if (notificationsJson != null) {
				try {
					List<Notification> notificationList = gson.fromJson(notificationsJson, type);
					notifications = new LimitedSizeQueue<>();
					notifications.addAll(notificationList);
				} catch (Exception ex) {}
            }
            if (notifications == null) {
				notifications = new LimitedSizeQueue<>();
			}
			notifications.setCapacity(maximumNotifications);

			notifications.add(notification);

            editor.remove(CleverPushPreferences.NOTIFICATIONS_JSON).apply();
			editor.remove(CleverPushPreferences.NOTIFICATIONS).apply();
            editor.putString(CleverPushPreferences.NOTIFICATIONS_JSON, gson.toJson(notifications, type));
            editor.putString(CleverPushPreferences.LAST_NOTIFICATION_ID, notificationId);
            editor.commit();
        } catch (Exception e) {
            Log.e("CleverPush", "Error saving notification to shared preferences", e);
        }
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
