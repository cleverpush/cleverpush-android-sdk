package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.cleverpush.BadgeHelper;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationAction;
import com.cleverpush.NotificationCarouselItem;
import com.cleverpush.NotificationCategory;
import com.cleverpush.NotificationOpenedActivity;
import com.cleverpush.NotificationStyle;
import com.cleverpush.R;
import com.cleverpush.Subscription;
import com.cleverpush.util.Logger;
import com.cleverpush.util.NotificationCategorySetUp;
import com.cleverpush.util.VoucherCodeUtils;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class NotificationService {
  private static NotificationService sInstance;

  private final int GET_BITMAP_TIMEOUT = 20 * 1000;

  private NotificationService() {

  }

  public static NotificationService getInstance() {
    if (sInstance == null) {
      sInstance = new NotificationService();
    }

    return sInstance;
  }

  private int getDrawableId(Context context, String name) {
    try {
      if (name != null && !name.isEmpty()) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
      } else {
        return 0;
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error in NotificationService getDrawableId: " + e.getLocalizedMessage());
      return 0;
    }
  }

  private int getSmallIcon(Context context) {
    int id = getDrawableId(context, "cleverpush_notification_icon");
    if (id != 0) {
      return id;
    }
    return getDrawableId(context, "default_notification_icon");
  }

  private Bitmap getBitmapFromUrl(String strURL) {
    try {
      URL url = new URL(strURL);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setConnectTimeout(GET_BITMAP_TIMEOUT);
      connection.setReadTimeout(GET_BITMAP_TIMEOUT);
      connection.setDoInput(true);
      connection.connect();
      InputStream input = connection.getInputStream();
      return BitmapFactory.decodeStream(input);
    } catch (Exception exception) {
      Logger.d(LOG_TAG, "NotificationService: Exception while loading image", exception);
      return null;
    }
  }

  private Intent getTargetIntent(Context context) {
    Intent targetIntent = new Intent(context, NotificationOpenedActivity.class);
    targetIntent.setFlags(
        Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_MULTIPLE_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    return targetIntent;
  }

  private int getPendingIntentFlags() {
    int flags = PendingIntent.FLAG_UPDATE_CURRENT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
    }
    return flags;
  }

  private int getDeleteIntentFlags() {
    int flags = PendingIntent.FLAG_ONE_SHOT;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      flags |= PendingIntent.FLAG_IMMUTABLE;
    }
    return flags;
  }

  private int generateRequestCode() {
    return (int) System.currentTimeMillis();
  }

  private NotificationCompat.Builder createBasicNotification(Context context, String notificationStr,
                                                             String subscriptionStr, Notification notification,
                                                             int requestId) {
    String voucherCode = notification.getVoucherCode();
    String iconUrl = notification.getIconUrl();
    String mediaUrl = notification.getMediaUrl();
    String title = VoucherCodeUtils.replaceVoucherCodeString(notification.getTitle(), voucherCode);
    String text = VoucherCodeUtils.replaceVoucherCodeString(notification.getText(), voucherCode);

    Intent targetIntent = this.getTargetIntent(context);
    targetIntent.putExtra("notification", notificationStr);
    targetIntent.putExtra("subscription", subscriptionStr);

    PendingIntent contentIntent =
        PendingIntent.getActivity(context, requestId, targetIntent, this.getPendingIntentFlags());

    NotificationStyle notificationStyle = getNotificationStyle(context);

    Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

    NotificationCompat.Builder notificationBuilder;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      if (notification.getCategory() != null) {
        NotificationCategory category = notification.getCategory();

        ArrayList<NotificationCategory> notificationCategories = new ArrayList<>();
        notificationCategories.add(category);

        NotificationCategorySetUp.setNotificationCategory(context, notificationCategories);

        notificationBuilder = new NotificationCompat.Builder(context, category.getId());

        String foregroundColor = category.getForegroundColor();
        if (foregroundColor != null) {
          int parsedForegroundColor = NotificationCategorySetUp.parseColor(foregroundColor);
          if (parsedForegroundColor != 0) {
            notificationBuilder.setColor(parsedForegroundColor);
          }
        }

      } else {
        NotificationChannel channel;
        if (notification.getNotificationChannel() != null) {
          channel = notification.getNotificationChannel();
        } else {
          int importance = NotificationManager.IMPORTANCE_DEFAULT;
          channel = new NotificationChannel("default", "Default", importance);
        }

        channel.setDescription(channel.getName().toString());
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        notificationBuilder = new NotificationCompat.Builder(context, channel.getId());
      }

    } else {
      notificationBuilder = new NotificationCompat.Builder(context);
    }

    if (notification.getSoundFilename() != null && !notification.getSoundFilename().isEmpty()) {
      Resources resources = context.getResources();
      String packageName = context.getPackageName();
      int soundId = resources.getIdentifier(notification.getSoundFilename(), "raw", packageName);
      if (soundId != 0) {
        Uri trySoundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + soundId);
        if (trySoundUri != null) {
          soundUri = trySoundUri;
        }
      }
    }

    notificationBuilder = notificationBuilder
        .setContentIntent(contentIntent)
        .setDeleteIntent(this.getNotificationDeleteIntent(context, notification))
        .setContentTitle(title)
        .setContentText(text)
        .setSmallIcon(getSmallIcon(context))
        .setAutoCancel(true)
        .setSound(soundUri);

    if (notification.getActions() != null && notification.getActions().length > 0) {
      List<NotificationCompat.Action> actions = new ArrayList<>();

      int maxActions = Math.min(notification.getActions().length, 2); // Limit to 2 buttons

      for (int i = 0; i < maxActions; i++) {
        NotificationAction action = notification.getActions()[i];

        // Create a PendingIntent for the button click
        PendingIntent actionPendingIntent = createActionPendingIntent(context, action, notification, subscriptionStr, i, requestId);

        NotificationCompat.Action notificationAction = new NotificationCompat.Action.Builder(
                getDrawableId(context, action.getIcon()), action.getTitle(), actionPendingIntent)
                .build();
        actions.add(notificationAction);
      }
      for (NotificationCompat.Action action : actions) {
        notificationBuilder.addAction(action);
      }
    }

    if (iconUrl != null && !iconUrl.isEmpty()) {
      try {
        Bitmap icon = getBitmapFromUrl(iconUrl);
        if (icon != null) {
          notificationBuilder = notificationBuilder.setLargeIcon(icon);
        }
      } catch (Exception exception) {
        Logger.e(LOG_TAG, "NotificationService: Error getting icon", exception);
      }
    }

    boolean hasMedia = mediaUrl != null && !mediaUrl.isEmpty();
    boolean hasText = text != null && text.length() > 0;
    if (notificationStyle == NotificationStyle.BIG_PICTURE || (
        notificationStyle == NotificationStyle.AUTO && hasMedia
    )) {
      try {
        Bitmap media = getBitmapFromUrl(mediaUrl);
        if (media != null) {
          notificationBuilder = notificationBuilder.setStyle(
              new NotificationCompat.BigPictureStyle().bigPicture(media)
          );
        }
      } catch (Exception exception) {
        Logger.e(LOG_TAG, "NotificationService: Error getting media", exception);
      }
    } else if (notificationStyle == NotificationStyle.BIG_TEXT || (
        notificationStyle == NotificationStyle.AUTO && hasText
    )) {
      notificationBuilder = notificationBuilder.setStyle(
          new NotificationCompat.BigTextStyle().bigText(text)
      );
    } else if (notificationStyle == NotificationStyle.TEXT_WITH_IMAGE) {
      RemoteViews remoteViews = this.getTextWithImageViews(context, notification, hasMedia);
      notificationBuilder = notificationBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());
      notificationBuilder = notificationBuilder.setCustomBigContentView(remoteViews);
    }

    // from NotificationExtenderService
    if (notification.getExtender() != null) {
      notificationBuilder.extend(notification.getExtender());
    }

    return notificationBuilder;
  }

  private RemoteViews getTextWithImageViews(Context context, Notification notification, boolean hasMedia) {
    RemoteViews expandedView = new RemoteViews(context.getPackageName(), R.layout.notification_text_image_layout);
    String voucherCode = notification.getVoucherCode();
    String title = VoucherCodeUtils.replaceVoucherCodeString(notification.getTitle(), voucherCode);
    String text = VoucherCodeUtils.replaceVoucherCodeString(notification.getText(), voucherCode);

    expandedView.setTextViewText(R.id.notification_title, title);
    expandedView.setTextViewText(R.id.notification_text, text);
    if (hasMedia) {
      try {
        Bitmap media = getBitmapFromUrl(notification.getMediaUrl());
        if (media != null) {
          expandedView.setImageViewBitmap(R.id.notification_image, media);
        }
      } catch (Exception exception) {
        Logger.e(LOG_TAG, "NotificationService getTextWithImageViews: Error getting media", exception);
      }
    }
    return expandedView;
  }

  int getRequestId(Context context, Notification notification) {
    // check for existing notifications which have the same tag and should be replaced. If found, use their request code.
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        StatusBarNotification[] activeNotifications = BadgeHelper.getActiveNotifications(context);
        for (StatusBarNotification activeNotification : activeNotifications) {
          if (activeNotification.getTag() != null && notification.getTag() != null && activeNotification.getTag()
              .equals(notification.getTag())) {
            return activeNotification.getId();
          }
        }
      }
    } catch (Exception exception) {
      Logger.d(LOG_TAG, "NotificationService: Exception while getting requestId", exception);
    }

    // We'll generate a random int and use it as the notification's request code.
    Random random = new SecureRandom();
    return random.nextInt();
  }

  NotificationStyle getNotificationStyle(Context context) {
    try {
      SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
      String notificationStyleCode = sharedPreferences.getString(CleverPushPreferences.NOTIFICATION_STYLE, null);
      if (notificationStyleCode != null) {
        return NotificationStyle.lookupByCode(notificationStyleCode);
      }
    } catch (Exception exception) {
      Logger.e(LOG_TAG, "NotificationService getNotificationStyle: Error getting notificationStyleCode", exception);
    }

    return NotificationStyle.AUTO;
  }

  int showNotification(Context context, Notification notification, Subscription subscription) {
    String notificationStr = notification.getRawPayload();
    String subscriptionStr = subscription.getRawPayload();

    int requestId;
    if (notification.getCarouselLength() > 0 && notification.isCarouselEnabled()) {
      requestId = NotificationService.getInstance()
          .createAndShowCarousel(context, notification, notificationStr, subscriptionStr);
    } else {
      requestId =
          NotificationService.getInstance().sendNotification(context, notification, notificationStr, subscriptionStr);
    }

    boolean badgeEnabled = notification.getCategory() == null || !notification.getCategory().getBadgeDisabled();
    if (badgeEnabled) {
      BadgeHelper.update(context, CleverPush.getInstance(context).getIncrementBadge());
    }

    return requestId;
  }

  int sendNotification(Context context, Notification notification, String notificationStr, String subscriptionStr) {
    int requestId = getRequestId(context, notification);
    NotificationCompat.Builder notificationBuilder = NotificationService.getInstance()
        .createBasicNotification(context, notificationStr, subscriptionStr, notification, requestId);
    if (notificationBuilder != null) {
      NotificationManagerCompat.from(context).notify(notification.getTag(), requestId, notificationBuilder.build());
    }
    return requestId;
  }

  int createAndShowCarousel(Context context, Notification message, String notificationStr, String subscriptionStr) {
    int requestId = getRequestId(context, message);
    createAndShowCarousel(context, message, notificationStr, subscriptionStr, 0, requestId);
    return requestId;
  }

  void createAndShowCarousel(Context context, Notification message, String notificationStr, String subscriptionStr,
                             int targetIndex, int requestId) {
    Logger.d(LOG_TAG, "NotificationService: createAndShowCarousel");
    NotificationCompat.Builder builder =
        createBasicNotification(context, notificationStr, subscriptionStr, message, requestId);
    if (builder != null) {
      android.app.Notification notification = builder.build();

      notification.bigContentView =
          getCarouselImage(context, message, notificationStr, subscriptionStr, targetIndex, requestId);

      builder.setDeleteIntent(getCarouselNotificationDeleteIntent(context, message, notificationStr, subscriptionStr));

      NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
      if (manager != null) {
        manager.notify(message.getTag(), requestId, notification);
      }
    }
  }

  private PendingIntent getNotificationDeleteIntent(Context context, Notification notification) {
    try {
      Intent delIntent = new Intent(context, NotificationDismissIntentService.class);

      try {
        delIntent.putExtra("notification", notification);
      } catch (Exception exception) {
        Logger.e(LOG_TAG, "NotificationService: Error with delete intent", exception);
      }

      return PendingIntent.getService(context, this.generateRequestCode(), delIntent, this.getDeleteIntentFlags());
    } catch (Exception e) {
      Logger.e(LOG_TAG, "getNotificationDeleteIntent: Error with delete intent", e);
      return null;
    }
  }

  private PendingIntent getCarouselNotificationDeleteIntent(Context context, Notification message,
                                                            String notificationStr, String subscriptionStr) {
    Intent delIntent = new Intent(context, CarouselNotificationIntentService.class);
    delIntent.setAction(CarouselNotificationIntentService.ACTION_NOTIFICATION_DELETE);

    HashMap<String, String> data = new HashMap<>();
    data.put("notification", notificationStr);
    data.put("subscription", subscriptionStr);

    delIntent.putExtra("notification", message);
    delIntent.putExtra("data", data);

    return PendingIntent.getService(context, this.generateRequestCode(), delIntent, this.getDeleteIntentFlags());
  }

  private RemoteViews getCarouselImage(Context context, Notification message, String notificationStr,
                                       String subscriptionStr, int currentIndex, int requestId) {
    RemoteViews contentView = null;

    contentView = new RemoteViews(context.getPackageName(), R.layout.notification_carousel_layout);

    setBasicNotificationData(message, contentView);

    if (message != null && message.getCarouselLength() > 0) {
      contentView.setViewVisibility(R.id.big_picture, View.VISIBLE);

      if (message.getCarouselLength() > 1) {
        contentView.setViewVisibility(R.id.next_button, View.VISIBLE);
        contentView.setViewVisibility(R.id.prev_button, View.VISIBLE);
      }

      NotificationCarouselItem[] elements = message.getCarouselItems();
      if (currentIndex < elements.length) {
        NotificationCarouselItem item = elements[currentIndex];

        String imageFileName = getImageFileName(item.getMediaUrl());
        Bitmap bitmap = loadImageFromDisc(context, imageFileName);

        if (bitmap == null) {
          downloadCarouselImages(context, message);
          bitmap = loadImageFromDisc(context, imageFileName);
        }

        if (bitmap != null) {
          contentView.setImageViewBitmap(R.id.big_picture, bitmap);
        }

        contentView.setOnClickPendingIntent(
            R.id.big_picture,
            getCarouselImageClickPendingIntent(context, message, notificationStr, subscriptionStr, item, requestId)
        );

        contentView.setOnClickPendingIntent(
            R.id.next_button,
            getNavigationPendingIntent(context, message, notificationStr, subscriptionStr,
                message.getNextCarouselIndex(currentIndex), requestId)
        );

        contentView.setOnClickPendingIntent(
            R.id.prev_button,
            getNavigationPendingIntent(context, message, notificationStr, subscriptionStr,
                message.getPreviousCarouselIndex(currentIndex), requestId)
        );
      }
    }

    return contentView;
  }

  private PendingIntent getNavigationPendingIntent(Context context, Notification message, String notificationStr,
                                                   String subscriptionStr, int targetIndex, int requestId) {
    Logger.i(LOG_TAG, "NotificationService: getNavigationPendingIntent");

    Intent intent = new Intent(context, CarouselNotificationIntentService.class);
    intent.setAction(CarouselNotificationIntentService.ACTION_CAROUSEL_IMG_CHANGE);

    intent.putExtra("carouselIndex", targetIndex);
    intent.putExtra("notificationId", requestId);
    intent.putExtra("notification", message);

    HashMap<String, String> data = new HashMap<>();
    data.put("notification", notificationStr);
    data.put("subscription", subscriptionStr);
    intent.putExtra("data", data);

    return PendingIntent.getService(context, requestId, intent, this.getPendingIntentFlags());
  }

  private PendingIntent getCarouselImageClickPendingIntent(Context context, Notification message,
                                                           String notificationStr, String subscriptionStr,
                                                           NotificationCarouselItem element, int requestId) {
    Bundle bundle = new Bundle();
    bundle.putInt("notificationId", requestId);
    bundle.putSerializable("notification", message);

    Intent targetIntent = this.getTargetIntent(context);

    targetIntent.putExtra("notification", notificationStr);
    targetIntent.putExtra("subscription", subscriptionStr);

    return PendingIntent.getActivity(context, this.generateRequestCode(), targetIntent, this.getPendingIntentFlags());
  }

  private void setBasicNotificationData(Notification notification, RemoteViews contentView) {
    if (notification != null && contentView != null) {
      String voucherCode = notification.getVoucherCode();
      String title = VoucherCodeUtils.replaceVoucherCodeString(notification.getTitle(), voucherCode);
      String text = VoucherCodeUtils.replaceVoucherCodeString(notification.getText(), voucherCode);

      contentView.setTextViewText(R.id.notification_title, title);
      contentView.setTextViewText(R.id.notification_text, text);
    }
  }

  private static String getImageFileName(String url) {
    if (url == null) {
      return null;
    }

    return url.substring(url.lastIndexOf('/') + 1);
  }

  private static Bitmap scaleBitmapAndKeepRation(Bitmap targetBmp, int reqWidthInPixels, int reqHeightInPixels) {
    Matrix matrix = new Matrix();
    matrix.setRectToRect(new RectF(0, 0, targetBmp.getWidth(), targetBmp.getHeight()),
        new RectF(0, 0, reqWidthInPixels, reqHeightInPixels), Matrix.ScaleToFit.CENTER);
    return Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.getWidth(), targetBmp.getHeight(), matrix, true);
  }

  private static void downloadCarouselImages(Context context, Notification message) {
    if (context != null && message != null) {
      NotificationCarouselItem[] NotificationCarouselItems = message.getCarouselItems();
      if (NotificationCarouselItems != null) {
        for (NotificationCarouselItem element : NotificationCarouselItems) {
          FileOutputStream fileOutputStream = null;
          try {
            if (element != null) {
              URL imageURL = new URL(element.getMediaUrl());
              Bitmap bitmap = BitmapFactory.decodeStream(imageURL.openStream());

              DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
              int ivWidth = displayMetrics.widthPixels;

              bitmap = scaleBitmapAndKeepRation(bitmap, ivWidth, 90);

              String imageUrl = element.getMediaUrl();
              String fileName = getImageFileName(imageUrl);

              if (!TextUtils.isEmpty(fileName)) {
                if (bitmap != null) {
                  fileOutputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                  bitmap.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
                  fileOutputStream.close();
                }
              }
            }
          } catch (IOException e) {
            Logger.e(LOG_TAG, "NotificationService: Error while downloading carousel images", e);
          } finally {
            if (fileOutputStream != null) {
              try {
                fileOutputStream.close();
              } catch (IOException e) {
                Logger.e(LOG_TAG, "NotificationService: Error while closing fileOutputStream", e);
              }
            }
          }
        }
      }
    }
  }

  private static Bitmap loadImageFromDisc(Context context, String fileName) {
    Bitmap bitmap = null;

    File imageFile = context.getFileStreamPath(fileName);
    if (imageFile.exists()) {
      InputStream inputStream = null;
      try {
        inputStream = context.openFileInput(fileName);
        bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();
      } catch (FileNotFoundException e) {
        Logger.e(LOG_TAG, "NotificationService: loadImageFromDisc FileNotFoundException", e);
      } catch (IOException e) {
        Logger.e(LOG_TAG, "NotificationService: loadImageFromDisc IOException", e);
      } finally {
        if (inputStream != null) {
          try {
            inputStream.close();
          } catch (IOException e) {
            Logger.e(LOG_TAG, "NotificationService: Error while closing inputStream", e);
          }
        }
      }
    }

    return bitmap;
  }

  private PendingIntent createActionPendingIntent(Context context, NotificationAction action, Notification notification, String subscriptionStr, int actionIndex, int requestId) {
    Intent actionIntent = getTargetIntent(context);
    Notification actionNotification = notification.copy();
    if (action.getUrl() != null && !action.getUrl().isEmpty()) {
      actionNotification.setUrl(action.getUrl());
    }
    String notificationStr = new Gson().toJson(actionNotification);
    actionIntent.putExtra("actionIndex", String.valueOf(actionIndex));
    actionIntent.putExtra("notificationId", requestId);
    actionIntent.putExtra("notification", notificationStr);
    actionIntent.putExtra("subscription", subscriptionStr);

    int requestCode = generateRequestCode();

    return PendingIntent.getActivity(context, requestCode, actionIntent, this.getPendingIntentFlags());
  }
}
