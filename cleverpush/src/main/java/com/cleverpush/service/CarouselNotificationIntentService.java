package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

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

        Log.i(LOG_TAG, "CarouselNotificationIntentService: onHandleIntent: " + action);

        if (action == null) return;

        Notification message = null;
        try {
            message = (Notification) intent.getSerializableExtra("notification");
        } catch (Exception ignore) {
        }

        Map<?, ?> data = null;
        try {
            data = (Map<?, ?>) intent.getSerializableExtra("data");
        } catch (Exception ignore) {
        }

        if (message == null || data == null) {
            return;
        }

        switch (action) {
            case ACTION_CAROUSEL_IMG_CHANGE:
                int targetIndex = intent.getIntExtra("carouselIndex", 0);
                int notificationId = intent.getIntExtra("notificationId", 0);

                updateCarouselNotification(this, message, (String) data.get("notification"), (String) data.get("subscription"), targetIndex, notificationId);

                break;

            case ACTION_NOTIFICATION_DELETE:
                NotificationCarouselItem[] carouselElements = message.getCarouselItems();
                if (carouselElements != null && carouselElements.length > 0) {
                    for (NotificationCarouselItem  element : carouselElements) {
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

    private void updateCarouselNotification(Context context, Notification message, String notificationStr, String subscriptionStr, int newIndex, int notificationId) {
        NotificationService.getInstance()
                .createAndShowCarousel(context, message, notificationStr, subscriptionStr, newIndex, notificationId);
    }
}
