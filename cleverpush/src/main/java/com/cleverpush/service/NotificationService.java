package com.cleverpush.service;

import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.RemoteViews;

import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationCarouselItem;
import com.cleverpush.NotificationOpenedActivity;
import com.cleverpush.NotificationOpenedReceiver;
import com.cleverpush.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService {
    private static NotificationService sInstance;

    private NotificationService() {

    }

    public static NotificationService getInstance() {
        if (sInstance == null) {
            sInstance = new NotificationService();
        }

        return sInstance;
    }

    private int getDrawableId(Context context, String name) {
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName());
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
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    boolean applicationInForeground(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> services = activityManager.getRunningAppProcesses();
        boolean isActivityFound = false;

        if (services.get(0).processName
                .equalsIgnoreCase(context.getPackageName()) && services.get(0).importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            isActivityFound = true;
        }

        return isActivityFound;
    }

    private NotificationCompat.Builder createBasicNotification(Context context, Map data, Notification notification, int requestCode) {
        boolean isBroadcast = false;
        Class<?> notificationOpenedClass;

        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(context, NotificationOpenedReceiver.class);
        intent.setPackage(context.getPackageName());
        if (packageManager.queryBroadcastReceivers(intent, 0).size() > 0) {
            isBroadcast = true;
            notificationOpenedClass = NotificationOpenedReceiver.class;
        } else {
            notificationOpenedClass = NotificationOpenedActivity.class;
        }

        String title = notification.getTitle();
        String text = notification.getText();
        String iconUrl = notification.getIconUrl();
        String mediaUrl = notification.getMediaUrl();

        if (title == null) {
            Log.e("CleverPush", "Notification title is empty");
            return null;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);

        Intent targetIntent = new Intent(context, notificationOpenedClass);
        if (!isBroadcast) {
            targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        targetIntent.putExtra("notification", (String) data.get("notification"));
        targetIntent.putExtra("subscription", (String) data.get("subscription"));

        PendingIntent contentIntent;
        if (isBroadcast) {
            contentIntent = PendingIntent.getBroadcast(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            contentIntent = PendingIntent.getActivity(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default", "Default", importance);
            channel.setDescription("default");
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new NotificationCompat.Builder(context, "default");
        } else {
            notificationBuilder = new NotificationCompat.Builder(context);
        }

        notificationBuilder = notificationBuilder
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(getSmallIcon(context))
                .setAutoCancel(true)
                .setSound(defaultSoundUri);

        if (iconUrl == null) {
            iconUrl = "https://static.cleverpush.com/app/images/default-icon.png";
        }

        try {
            Bitmap icon = getBitmapFromUrl(iconUrl);
            if (icon != null) {
                notificationBuilder = notificationBuilder.setLargeIcon(icon);
            }
        } catch (Exception ignored) {
        }

        if (mediaUrl != null) {
            try {
                Bitmap media = getBitmapFromUrl(mediaUrl);
                if (media != null) {
                    notificationBuilder = notificationBuilder.setStyle(
                            new NotificationCompat.BigPictureStyle().bigPicture(media)
                    );
                }
            } catch (Exception ignored) {
            }
        } else if (text != null && text.length() > 0) {
            notificationBuilder.setStyle(
                    new NotificationCompat.BigTextStyle().bigText(text)
            );
        }

        return notificationBuilder;
    }

    void sendNotification(Context context, Notification notification, Map data) {
        int requestCode = (int) System.currentTimeMillis();
        NotificationCompat.Builder notificationBuilder = NotificationService.getInstance().createBasicNotification(context, data, notification, requestCode);
        if (notificationBuilder != null) {
            NotificationManagerCompat.from(context).notify(requestCode, notificationBuilder.build());
        }
    }

    void createAndShowCarousel(Context context, Notification message, Map data) {
        int requestId = (int) System.currentTimeMillis();
        createAndShowCarousel(context, message, data, 0, requestId);
    }

    void createAndShowCarousel(Context context, Notification message, Map data, int targetIndex, int requestId) {
        Log.i("CleverPush", "NotificationService: createAndShowCarousel");
        NotificationCompat.Builder builder = createBasicNotification(context, data, message, requestId);
        if (builder != null) {
            android.app.Notification notification = builder.build();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                notification.bigContentView = getCarouselImage(context, message, data, targetIndex, requestId);
            }

            builder.setDeleteIntent(getNotificationDeleteIntent(context, message, data));

            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(requestId, notification);
            }
        }
    }

    private PendingIntent getNotificationDeleteIntent(Context context, Notification message, Map data) {
        Intent delIntent = new Intent(context, CarouselNotificationIntentService.class);
        delIntent.setAction(CarouselNotificationIntentService.ACTION_NOTIFICATION_DELETE);

        delIntent.putExtra("notification", message);
        delIntent.putExtra("data", (data instanceof HashMap)
                ? (HashMap) data
                : new HashMap<String, String>(data));

        return PendingIntent.getService(context, (int) System.currentTimeMillis(), delIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    private RemoteViews getCarouselImage(Context context, Notification message, Map data, int currentIndex, int requestId) {
        RemoteViews contentView = null;

        contentView = new RemoteViews(context.getPackageName(), R.layout.notification_carousel_layout);

        setBasicNotificationData(context, message, contentView, true);

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
                        getCarouselImageClickPendingIntent(context, message, data, item, requestId)
                );

                contentView.setOnClickPendingIntent(
                        R.id.next_button,
                        getNavigationPendingIntent(context, message, data, message.getNextCarouselIndex(currentIndex), requestId)
                );

                contentView.setOnClickPendingIntent(
                        R.id.prev_button,
                        getNavigationPendingIntent(context, message, data, message.getPreviousCarouselIndex(currentIndex), requestId)
                );
            }
        }

        return contentView;
    }

    private PendingIntent getNavigationPendingIntent(Context context, Notification message, Map data, int targetIndex, int requestId) {
        Log.i("CleverPush", "NotificationService: getNavigationPendingIntent");

        Intent intent = new Intent(context, CarouselNotificationIntentService.class);
        intent.setAction(CarouselNotificationIntentService.ACTION_CAROUSEL_IMG_CHANGE);

        intent.putExtra("carouselIndex", targetIndex);
        intent.putExtra("notificationId", requestId);
        intent.putExtra("notification", message);
        intent.putExtra("data", (data instanceof HashMap)
                ? (HashMap) data
                : new HashMap<String, String>(data));

        return PendingIntent.getService(context, requestId, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private PendingIntent getCarouselImageClickPendingIntent(Context context, Notification message, Map data, NotificationCarouselItem element, int requestId) {
        Bundle bundle = new Bundle();
        bundle.putInt("notificationId", requestId);
        bundle.putSerializable("notification", message);

        boolean isBroadcast = false;
        Class<?> notificationOpenedClass;

        PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(context, NotificationOpenedReceiver.class);
        intent.setPackage(context.getPackageName());
        if (packageManager.queryBroadcastReceivers(intent, 0).size() > 0) {
            isBroadcast = true;
            notificationOpenedClass = NotificationOpenedReceiver.class;
        } else {
            notificationOpenedClass = NotificationOpenedActivity.class;
        }

        Intent targetIntent = new Intent(context, notificationOpenedClass);
        if (!isBroadcast) {
            targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        targetIntent.putExtra("notification", (String) data.get("notification"));
        targetIntent.putExtra("subscription", (String) data.get("subscription"));

        int requestCode = (int) System.currentTimeMillis();

        PendingIntent contentIntent;
        if (isBroadcast) {
            contentIntent = PendingIntent.getBroadcast(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            contentIntent = PendingIntent.getActivity(context, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        return contentIntent;
    }

    private void setBasicNotificationData(Context context, Notification message, RemoteViews contentView, boolean isExpanded) {
        if (message != null && contentView != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                contentView.setViewVisibility(R.id.icon_group, View.GONE);

                int smallIconResId = this.getSmallIcon(context);
                if (smallIconResId != 0) {
                    contentView.setViewVisibility(R.id.notification_small_icon, View.VISIBLE);
                    contentView.setImageViewResource(R.id.notification_small_icon, smallIconResId);
                }
            }

            contentView.setTextViewText(R.id.notification_content_title, message.getTitle());
            contentView.setTextViewText(R.id.notification_content_text, message.getText());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    contentView.setTextViewTextSize(R.id.notification_content_title, TypedValue.COMPLEX_UNIT_SP, 14);
                    contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, 14);
                } else {
                    contentView.setTextViewTextSize(R.id.notification_content_title, TypedValue.COMPLEX_UNIT_SP, 14);
                    contentView.setTextViewTextSize(R.id.notification_content_text, TypedValue.COMPLEX_UNIT_SP, 14);
                }
            }
        }
    }

    private static String getImageFileName(String url) {
        if (url == null) return null;

        return url.substring(url.lastIndexOf('/') + 1);
    }

    private static Bitmap resizeImageForDevice(Context context, Bitmap sourceBitmap) {
        Bitmap resizedBitmap = null;

        if (sourceBitmap != null) {
            if (sourceBitmap.getWidth() > sourceBitmap.getHeight()) {
                DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();

                int newWidth = displayMetrics.widthPixels;
                int newHeight = newWidth / 2;

                resizedBitmap = scaleBitmapAndKeepRation(sourceBitmap, newWidth, newHeight);
            }
        }

        if (resizedBitmap == null) {
            resizedBitmap = sourceBitmap;
        }

        return resizedBitmap;
    }

    private static Bitmap scaleBitmapAndKeepRation(Bitmap targetBmp, int reqWidthInPixels, int reqHeightInPixels) {
        Matrix matrix = new Matrix();
        matrix.setRectToRect(new RectF(0, 0, targetBmp.getWidth(), targetBmp.getHeight()), new RectF(0, 0, reqWidthInPixels, reqHeightInPixels), Matrix.ScaleToFit.CENTER);
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
                            int currentBitmapWidth = bitmap.getWidth();
                            int currentBitmapHeight = bitmap.getHeight();
                            int newHeight = (int) Math.floor((double) currentBitmapHeight * ((double) ivWidth / (double) currentBitmapWidth));

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
                        Log.e("CleverPush", e.getMessage());
                    } finally {
                        if (fileOutputStream != null) {
                            try {
                                fileOutputStream.close();
                            } catch (IOException e) {
                                Log.e("CleverPush", e.getMessage());
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
                Log.e("CleverPush", e.getMessage());
            } catch (IOException e) {
                Log.e("CleverPush", e.getMessage());
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("CleverPush", e.getMessage());
                    }
                }
            }
        }

        return bitmap;
    }
}
