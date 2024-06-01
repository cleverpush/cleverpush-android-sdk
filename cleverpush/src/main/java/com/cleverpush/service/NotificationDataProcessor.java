package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;
import static com.cleverpush.service.NotificationExtenderService.EXTENDER_SERVICE_JOB_ID;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.NotificationReceivedEvent;
import com.cleverpush.NotificationServiceExtension;
import com.cleverpush.Subscription;
import com.cleverpush.util.LifecycleUtils;
import com.cleverpush.util.LimitedSizeQueue;
import com.cleverpush.util.Logger;
import com.cleverpush.util.MetaDataUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.Map;

public class NotificationDataProcessor {
  public static int maximumNotifications = 100;
  public static ExecutorService executor = Executors.newSingleThreadExecutor();
  private static NotificationServiceExtension extension = null;

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
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    SharedPreferences.Editor editor = sharedPreferences.edit();

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
        if (LifecycleUtils.applicationInForeground(context)) {
          dontShowNotification = cleverPush.fireNotificationReceivedListener(result);
        } else {
          cleverPush.fireNotificationReceivedListener(result);
        }
      }

    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error checking if application is in foreground", e);
    }

    // do not show silent notifications
    if (notification.isSilent()) {
      dontShowNotification = true;

      handleSilentNotificationBanner(notification, sharedPreferences, editor);
    }

    boolean hasExtenderService = startExtenderService(context, notification, subscription);
    if (hasExtenderService) {
      dontShowNotification = true;
    }

    boolean hasServiceExtension = startServiceExtension(context, notification, subscription);
    if (hasServiceExtension) {
      dontShowNotification = true;
    }

    if (!dontShowNotification) {
      NotificationService.getInstance().showNotification(context, notification, subscription);
    }

    try {
      if (maximumNotifications <= 0) {
        return;
      }

      Gson gson = new Gson();
      String notificationsJson = sharedPreferences.getString(CleverPushPreferences.NOTIFICATIONS_JSON, null);
      Type type = new TypeToken<List<Notification>>() {
      }.getType();

      LimitedSizeQueue<Notification> notifications = null;
      if (notificationsJson != null) {
        try {
          List<Notification> notificationList = gson.fromJson(notificationsJson, type);
          notifications = new LimitedSizeQueue<>();
          notifications.addAll(notificationList);
        } catch (Exception ex) {
          Logger.e(LOG_TAG, "Error creating notificationList", ex);
        }
      }
      if (notifications == null) {
        notifications = new LimitedSizeQueue<>();
      }
      notifications.setCapacity(maximumNotifications);
      if (notification.getCreatedAt() == null || notification.getCreatedAt().equalsIgnoreCase("")) {
        String currentDate = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
          currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX", Locale.US).format(new Date());
        } else {
          currentDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(new Date());
        }
        notification.setCreatedAt(currentDate);
      }

      notifications.add(notification);

      editor.remove(CleverPushPreferences.NOTIFICATIONS_JSON).apply();
      editor.remove(CleverPushPreferences.NOTIFICATIONS).apply();
      editor.putString(CleverPushPreferences.NOTIFICATIONS_JSON, gson.toJson(notifications, type));
      editor.putString(CleverPushPreferences.LAST_NOTIFICATION_ID, notificationId);
      editor.apply();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error saving notification to shared preferences", e);
    }
  }

  private static boolean startExtenderService(Context context, Notification notification, Subscription subscription) {
    Intent intent = NotificationExtenderService.getIntent(context);
    if (intent == null) {
      return false;
    }

    Logger.w(LOG_TAG, "NotificationExtenderService is deprecated. Please migrate to NotificationServiceExtension. " +
            "\nRefer to the documentation for more information: " +
            "https://developers.cleverpush.com/docs/sdks/android/extension");

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

  public static void setupNotificationServiceExtension(Context context) {
    String className = MetaDataUtils.getNotificationServiceExtensionClass(context);

    if (className == null) {
      return;
    }

    try {
      Class<?> clazz = Class.forName(className);
      Object clazzInstance = clazz.newInstance();

      if (clazzInstance instanceof NotificationServiceExtension && extension == null) {
        extension = (NotificationServiceExtension) clazzInstance;
      }
    } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
      Logger.e(LOG_TAG, "Error while setting up NotificationServiceExtension: " + e.getMessage(), e);
    }
  }

  private static boolean startServiceExtension(Context context, Notification notification, Subscription subscription) {
    NotificationDataProcessor.setupNotificationServiceExtension(context);
    if (extension == null) {
      Logger.d(LOG_TAG, "startServiceExtension: Extension is NULL. returning");
      return false;
    }

    AtomicBoolean wantsToDisplay = new AtomicBoolean(true);

    NotificationReceivedEvent notificationReceivedEvent = new NotificationReceivedEvent(context, notification);

    Future<?> future = executor.submit(() -> {
      extension.onNotificationReceived(notificationReceivedEvent);

      if (notificationReceivedEvent.isPreventDefault()) {
        wantsToDisplay.set(false);
      }
    });

    try {
      future.get(30, TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException | TimeoutException e) {
      future.cancel(true);
    }

    if (wantsToDisplay.get() && !notification.isSilent()) {
      NotificationService.getInstance().showNotification(context, notification, subscription);
    }

    return true;
  }

  private static void handleSilentNotificationBanner(Notification notification, SharedPreferences sharedPreferences, SharedPreferences.Editor editor) {
    if (notification.getAppBanner() != null && !notification.getAppBanner().isEmpty()) {
      String silentPushBanners = sharedPreferences.getString(CleverPushPreferences.SILENT_PUSH_APP_BANNER, null);
      Map<String, String> silentPushBannersMap;

      if (silentPushBanners != null) {
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        silentPushBannersMap = new Gson().fromJson(silentPushBanners, type);
      } else {
        silentPushBannersMap = new HashMap<>();
      }

      if (!silentPushBannersMap.containsKey(notification.getId())) {
        silentPushBannersMap.put(notification.getId(), notification.getAppBanner());
        editor.putString(CleverPushPreferences.SILENT_PUSH_APP_BANNER, new Gson().toJson(silentPushBannersMap));
        editor.apply();
      }
    }
  }

}
