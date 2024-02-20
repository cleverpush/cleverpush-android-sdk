package com.cleverpush.cleverpush_example_android;

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
