package com.cleverpush.util;

import static android.graphics.Color.parseColor;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.cleverpush.NotificationCategory;
import com.cleverpush.NotificationCategoryGroup;

import java.util.ArrayList;

public class NotificationCategorySetUp {
    private ArrayList<NotificationCategory> notificationCategories;
    private Context context;

    public NotificationCategorySetUp(Context context, ArrayList<NotificationCategory> notificationCategories) {
        this.notificationCategories = notificationCategories;
        this.context = context;
    }

    public void setNotificationCategory() {

        for (int i = 0; i < notificationCategories.size(); i++) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationCategory category = notificationCategories.get(i);
                NotificationChannel channel = new NotificationChannel(category.getId(), category.getName(), NotificationManager.IMPORTANCE_DEFAULT);

                String description = category.getDescription();
                if (description != null) {
                    channel.setDescription(description);
                }

                String importance = category.getImportance();
                if (importance != null) {
                    if (importance.equalsIgnoreCase("URGENT")) {
                        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
                    } else if (importance.equalsIgnoreCase("HIGH")) {
                        channel.setImportance(NotificationManager.IMPORTANCE_HIGH);
                    } else if (importance.equalsIgnoreCase("MEDIUM")) {
                        channel.setImportance(NotificationManager.IMPORTANCE_DEFAULT);
                    } else if (importance.equalsIgnoreCase("LOW")) {
                        channel.setImportance(NotificationManager.IMPORTANCE_LOW);
                    }
                }

                String lockScreen = category.getLockScreen();
                if (lockScreen != null) {
                    if (lockScreen.equalsIgnoreCase("PUBLIC")) {
                        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
                    } else if (lockScreen.equalsIgnoreCase("PRIVATE")) {
                        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
                    } else if (lockScreen.equalsIgnoreCase("SECRET")) {
                        channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
                    }
                }

                String ledColor = category.getLedColor();
                if (category.getLedColorEnabled() && ledColor != null) {
                    int parsedLedColor = parseColor(ledColor);
                    if (parsedLedColor > 0) {
                        channel.setLightColor(parsedLedColor);
                    }
                }

                NotificationManager notificationManager = context.getSystemService(NotificationManager.class);

                NotificationCategoryGroup categoryGroup = category.getGroup();
                if (categoryGroup != null) {
                    NotificationChannelGroup group = new NotificationChannelGroup(categoryGroup.getId(), categoryGroup.getName());
                    notificationManager.createNotificationChannelGroup(group);
                    channel.setGroup(group.getId());
                }

                notificationManager.createNotificationChannel(channel);
            }
        }

    }
}
