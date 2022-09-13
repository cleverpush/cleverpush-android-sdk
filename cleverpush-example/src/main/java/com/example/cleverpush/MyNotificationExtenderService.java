package com.example.cleverpush;

import androidx.core.app.NotificationCompat;
import com.cleverpush.Notification;
import com.cleverpush.service.NotificationExtenderService;

import java.math.BigInteger;

public class MyNotificationExtenderService extends NotificationExtenderService {
    @Override
    protected boolean onNotificationProcessing(Notification notification) {
        // modify notification
        notification.setExtender(new NotificationCompat.Extender() {
            @Override
            public NotificationCompat.Builder extend(NotificationCompat.Builder builder) {
                builder.setColor(new BigInteger("FF00FF00", 16).intValue()); // Set notification color to green
                return builder;
            }
        });

        // return true to not display notification
        return false;
    }
}
