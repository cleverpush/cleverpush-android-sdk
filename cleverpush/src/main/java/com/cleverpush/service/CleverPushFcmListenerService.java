package com.cleverpush.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.NotificationOpenedActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class CleverPushFcmListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map data = message.getData();
        if (data.size() > 0) {
            String notificationId = (String) data.get("notificationId");
            String subscriptionId = (String) data.get("subscriptionId");

            if (notificationId == null || subscriptionId == null) {
                return;
            }

            sendNotification(data);

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("notificationId", notificationId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            CleverPushHttpClient.post("notification/delivered", jsonBody, null);
        }
    }

    private void sendNotification(Map data) {
        String title = (String) data.get("title");
        String text = (String) data.get("text");
        String iconUrl = (String) data.get("iconUrl");

        if (title == null) {
            return;
        }

        Intent targetIntent = new Intent(this, NotificationOpenedActivity.class);
        JSONObject json = new JSONObject(data);
        targetIntent.putExtra("data", json.toString());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                .setContentIntent(contentIntent)
                .setContentTitle(title)
                .setContentText(text)
                .setLargeIcon(getBitmapFromUrl(iconUrl))
                .setAutoCancel(true)
                .setSound(defaultSoundUri);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(0, notificationBuilder.build());
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
}
