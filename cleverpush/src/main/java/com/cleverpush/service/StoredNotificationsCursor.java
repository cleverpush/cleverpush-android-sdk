package com.cleverpush.service;

import android.content.SharedPreferences;

import com.cleverpush.Notification;
import com.cleverpush.listener.NotificationsPageCallbackListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class StoredNotificationsCursor {
    private final int limit;
    private final String channelId;
    private final SharedPreferences sharedPreferences;
    private int remoteOffset = 0;
    private int localOffset = 0;
    private boolean hasNextPage = true;
    private final Set<Notification> localNotifications;
    private final List<String> alreadyReturnedIds;

    public StoredNotificationsCursor(String channelId, SharedPreferences sharedPreferences, int limit) {
        this.channelId = channelId;
        this.sharedPreferences = sharedPreferences;
        this.limit = limit;
        this.alreadyReturnedIds = new ArrayList<>();
        this.localNotifications = StoredNotificationsService.getNotificationsFromLocal(sharedPreferences);
    }

    public void getNextPage(NotificationsPageCallbackListener notificationsCallbackListener) {
        StoredNotificationsService.getReceivedNotificationsFromApi(this.channelId, sharedPreferences, limit + localOffset, remoteOffset, (remoteNotifications) -> {
            this.returnCombinedNotifications(notificationsCallbackListener, localNotifications, remoteNotifications);
        });
    }

    public boolean hasNextPage() {
        return this.hasNextPage;
    }

    private List<Notification> safeSubList(List<Notification> list, int fromIndex, int toIndex) {
        if (fromIndex > toIndex) {
            return list;
        }
        try {
            return list.subList(fromIndex, toIndex);
        } catch (IndexOutOfBoundsException ex) {
            int newToIndex = list.size() - fromIndex;
            if (fromIndex > newToIndex) {
                return new ArrayList<>();
            }
            return list.subList(fromIndex, newToIndex);
        }
    }

    private void sortNotifications(List<Notification> notifications) {
        Collections.sort(notifications, new Comparator<Notification>() {
            public int compare(Notification notification1, Notification notification2) {
                return notification2.getCreatedAtDate().compareTo(notification1.getCreatedAtDate());
            }
        });
    }

    private void returnCombinedNotifications(NotificationsPageCallbackListener notificationsCallbackListener, Set<Notification> localNotifications, List<Notification> remoteNotifications) {
        List<Notification> localNotificationsList = new ArrayList<>(localNotifications);
        List<Notification> uniqueNotifications = new ArrayList<>();

        for (Notification notification : localNotificationsList) {
            if (notification.getCreatedAtTime() > 0) {
                uniqueNotifications.add(notification);
            }
        }

        this.sortNotifications(uniqueNotifications);

        uniqueNotifications = this.safeSubList(uniqueNotifications, localOffset + 1, localNotifications.size());

        for (Notification remoteNotification : remoteNotifications) {
            boolean isFound = false;
            for (Notification localNotification : uniqueNotifications) {
                if (
                        (localNotification.getTag() != null && localNotification.getTag().equals(remoteNotification.getId()))
                        || localNotification.getId().equals(remoteNotification.getId())
                ) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound && !alreadyReturnedIds.contains(remoteNotification.getId())) {
                remoteNotification.setFromApi(true);
                uniqueNotifications.add(remoteNotification);
            }
        }

        this.sortNotifications(uniqueNotifications);

        List<Notification> finalNotifications = this.safeSubList(uniqueNotifications, 0, this.limit);
        for (Notification notification : finalNotifications) {
            alreadyReturnedIds.add(notification.getId());
            if (notification.getFromApi()) {
                remoteOffset += 1;
            } else {
                localOffset += 1;
            }
        }

        hasNextPage = uniqueNotifications.size() >= this.limit;

        notificationsCallbackListener.ready(finalNotifications);
    }
}
