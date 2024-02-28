package com.cleverpush.cleverpush_example_android;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import com.cleverpush.NotificationReceivedEvent;
import com.cleverpush.NotificationServiceExtension;
import java.math.BigInteger;

public class MyNotificationServiceExtension implements NotificationServiceExtension {
    @Override
    public void onNotificationReceived(NotificationReceivedEvent event) {
        // call `event.preventDefault()` to not display notification
        // event.preventDefault();
        Log.i("CleverPush", "CleverPush MyNotificationServiceExtension onNotificationReceived");

        // to prevent the `default` notification channel creation, use `event.getNotification().setNotificationChannel()`
        /*if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel;
            String channelId = "channel_id"; // replace with your desired channel id
            CharSequence channelName = "Channel_Name"; // replace with your desired channel name
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            channel = new NotificationChannel(channelId, channelName, importance);
            event.getNotification().setNotificationChannel(channel);
        }*/

        // modify notification
        event.getNotification().setExtender(new NotificationCompat.Extender() {
            @Override
            public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                builder.setColor(new BigInteger("FF00FF00", 16).intValue()); // Set notification color to green
                return builder;
            }
        });
    }
}
