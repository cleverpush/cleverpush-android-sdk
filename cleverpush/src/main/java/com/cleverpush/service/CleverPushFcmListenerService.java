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
import android.graphics.drawable.Drawable;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.NotificationOpenedActivity;
import com.cleverpush.NotificationOpenedReceiver;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.Subscription;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CleverPushFcmListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Log.d("CleverPush", "onMessageReceived");

        Map data = message.getData();
        if (data.size() > 0) {
            Log.d("CleverPush", "Notification data: " + data.toString());

            Gson gson = new Gson();
            Notification notification = gson.fromJson((String) data.get("notification"), Notification.class);
            Subscription subscription = gson.fromJson((String) data.get("subscription"), Subscription.class);

            if (notification == null || subscription == null) {
                return;
            }

            String notificationId = notification.getId();
            String subscriptionId = subscription.getId();

            if (notificationId == null || subscriptionId == null) {
                return;
            }

            boolean foregroundWithReceivedListener = false;

            try {
                NotificationOpenedResult result = new NotificationOpenedResult();
                result.setNotification(notification);
                result.setSubscription(subscription);

                if (this.applicationInForeground()) {
                    foregroundWithReceivedListener = CleverPush.getInstance(null).fireNotificationReceivedListener(result);
                } else {
                    CleverPush.getInstance(null).fireNotificationReceivedListener(result);
                }
            } catch (Exception e) {
                Log.e("CleverPush", "Error checking if application is in foreground", e);
            }

            if (!foregroundWithReceivedListener) {
                sendNotification(notification, data);
            }

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("notificationId", notificationId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                Log.e("CleverPush", "Error generating delivered json", e);
            }

            CleverPushHttpClient.post("/notification/delivered", jsonBody, null);

            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                Set<String> notifications = sharedPreferences.getStringSet(CleverPushPreferences.NOTIFICATIONS, new HashSet<>());
                notifications.add(gson.toJson(notification));
                editor.remove(CleverPushPreferences.NOTIFICATIONS).apply();
                editor.putStringSet(CleverPushPreferences.NOTIFICATIONS, notifications);
                editor.commit();
            } catch (Exception e) {
                Log.e("CleverPush", "Error saving notification to shared preferences", e);
            }
        } else {
            Log.e("CleverPush", "Notification data is empty");
        }
    }

    private void sendNotification(Notification notification, Map data) {
        boolean isBroadcast = false;
        Class<?> notificationOpenedClass;

        PackageManager packageManager = this.getPackageManager();
        Intent intent = new Intent(this, NotificationOpenedReceiver.class);
        intent.setPackage(this.getPackageName());
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
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String channelId = sharedPreferences.getString(CleverPushPreferences.CHANNEL_ID, null);

        Intent targetIntent = new Intent(this, notificationOpenedClass);
        if (!isBroadcast) {
            targetIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        targetIntent.putExtra("notification", (String) data.get("notification"));
        targetIntent.putExtra("subscription", (String) data.get("subscription"));

        int requestCode = (int) System.currentTimeMillis();

        PendingIntent contentIntent;
        if (isBroadcast) {
            contentIntent = PendingIntent.getBroadcast(this, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        } else {
            contentIntent = PendingIntent.getActivity(this, requestCode, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        int defaultSmallIcon = this.getResources().getIdentifier("default_notification_icon", "drawable", this.getPackageName());

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder notificationBuilder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel("default", "Default", importance);
            channel.setDescription("default");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            notificationBuilder = new NotificationCompat.Builder(this, "default");
        } else {
            notificationBuilder = new NotificationCompat.Builder(this);
        }

        notificationBuilder = notificationBuilder
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(defaultSmallIcon)
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

        NotificationManagerCompat.from(this).notify(requestCode, notificationBuilder.build());
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

    private boolean applicationInForeground() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> services = activityManager.getRunningAppProcesses();
        boolean isActivityFound = false;

        if (services.get(0).processName
                .equalsIgnoreCase(getPackageName()) && services.get(0).importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
            isActivityFound = true;
        }

        return isActivityFound;
    }
}
