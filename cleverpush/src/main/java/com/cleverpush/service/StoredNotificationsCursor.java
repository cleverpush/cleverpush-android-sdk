package com.cleverpush.service;

import android.content.SharedPreferences;

import com.cleverpush.Notification;
import com.cleverpush.listener.NotificationsCallbackListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
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

    public void getNextPage(NotificationsCallbackListener notificationsCallbackListener) {
        StoredNotificationsService.getReceivedNotificationsFromApi(this.channelId, sharedPreferences, limit + localOffset, remoteOffset, (remoteNotifications) -> {
            this.returnCombinedNotifications(notificationsCallbackListener, localNotifications, remoteNotifications);
        });
    }

    public boolean hasNextPage() {
        return this.hasNextPage;
    }

    private List<Notification> safeSubList(List<Notification> list, int fromIndex, int toIndex) {
        try {
            return list.subList(fromIndex, toIndex);
        } catch (IndexOutOfBoundsException ex) {
            return list.subList(fromIndex, list.size() - fromIndex);
        }
    }

    private void returnCombinedNotifications(NotificationsCallbackListener notificationsCallbackListener, Set<Notification> localNotifications, List<Notification> remoteNotifications) {
        List<Notification> localNotificationsOffseted = this.safeSubList(new ArrayList<>(localNotifications), localOffset, localNotifications.size());
        List<Notification> uniqueNotifications = new ArrayList<>(localNotificationsOffseted);

        for (Notification notification : remoteNotifications) {
            boolean isFound = false;
            for (Notification tryNotification : uniqueNotifications) {
                if (tryNotification.getId().equals(notification.getId())) {
                    isFound = true;
                    break;
                }
            }
            if (!isFound && !alreadyReturnedIds.contains(notification.getId())) {
                notification.setFromApi(true);
                uniqueNotifications.add(notification);
            }
        }

        Collections.sort(uniqueNotifications, new Comparator<Notification>() {
            public int compare(Notification notification1, Notification notification2) {
                return notification2.getCreatedAt().compareTo(notification1.getCreatedAt());
            }
        });

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

        notificationsCallbackListener.ready(new HashSet<>(finalNotifications));
    }
}
