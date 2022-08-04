package com.cleverpush.listener;

import com.cleverpush.Notification;

import java.util.List;

public interface NotificationsPageCallbackListener {
    void ready(List<Notification> notifications);
}
