package com.cleverpush;

import java.util.ArrayList;
import java.util.List;

public class NotificationList extends ArrayList<com.cleverpush.Notification> {
    private List<com.cleverpush.Notification> Notification;

    public List<com.cleverpush.Notification> getNotification() {
        return Notification;
    }

    public void setNotification(List<com.cleverpush.Notification> notification) {
        Notification = notification;
    }
}
