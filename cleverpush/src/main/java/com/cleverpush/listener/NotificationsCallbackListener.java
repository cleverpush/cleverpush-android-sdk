package com.cleverpush.listener;

import com.cleverpush.Notification;

import java.util.Set;

public interface NotificationsCallbackListener {
  void ready(Set<Notification> notifications);
}
