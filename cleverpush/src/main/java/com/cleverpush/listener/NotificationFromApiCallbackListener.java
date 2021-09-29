package com.cleverpush.listener;

import com.cleverpush.Notification;

import java.util.List;

public interface NotificationFromApiCallbackListener {
    void ready(List<Notification> notifications);
}
