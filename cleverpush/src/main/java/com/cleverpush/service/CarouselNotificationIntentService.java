package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import com.cleverpush.util.Logger;

import com.cleverpush.Notification;
import com.cleverpush.NotificationCarouselItem;

import java.util.Map;

public class CarouselNotificationIntentService extends IntentService {
  public static final String ACTION_CAROUSEL_IMG_CHANGE = "carousel_image_change";
  public static final String ACTION_NOTIFICATION_DELETE = "notification_delete";

  public CarouselNotificationIntentService(String name) {
    super(name);
  }

  public CarouselNotificationIntentService() {
    super("NotificationIntentService");
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    String action = intent.getAction();

    Logger.i(LOG_TAG, "CarouselNotificationIntentService: onHandleIntent: " + action);

    if (action == null) {
      return;
    }

    Notification notification = null;
    try {
      notification = (Notification) intent.getSerializableExtra("notification");
    } catch (Exception ignore) {
    }

    Map<?, ?> data = null;
    try {
      data = (Map<?, ?>) intent.getSerializableExtra("data");
    } catch (Exception ignore) {
    }

    if (notification == null || data == null) {
      return;
    }

    switch (action) {
      case ACTION_CAROUSEL_IMG_CHANGE:
        int targetIndex = intent.getIntExtra("carouselIndex", 0);
        int notificationId = intent.getIntExtra("notificationId", 0);

        updateCarouselNotification(this, notification, (String) data.get("notification"),
            (String) data.get("subscription"), targetIndex, notificationId);

        break;

      case ACTION_NOTIFICATION_DELETE:
        NotificationCarouselItem[] carouselElements = notification.getCarouselItems();
        if (carouselElements != null && carouselElements.length > 0) {
          for (NotificationCarouselItem element : carouselElements) {
            String url = element.getMediaUrl();
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            if (!TextUtils.isEmpty(fileName)) {
              this.deleteFile(fileName);
            }
          }
        }

        break;
    }
  }

  private void updateCarouselNotification(Context context, Notification notification, String notificationStr,
                                          String subscriptionStr, int newIndex, int notificationId) {
    NotificationService.getInstance()
        .createAndShowCarousel(context, notification, notificationStr, subscriptionStr, newIndex, notificationId);
  }
}
