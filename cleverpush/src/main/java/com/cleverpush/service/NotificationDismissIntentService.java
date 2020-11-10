package com.cleverpush.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.cleverpush.BadgeHelper;
import com.cleverpush.CleverPush;
import com.cleverpush.Notification;
import com.cleverpush.NotificationCarouselItem;

import java.util.Map;

public class NotificationDismissIntentService extends IntentService {
	public NotificationDismissIntentService(String name) {
		super(name);
	}

	public NotificationDismissIntentService() {
		super("NotificationIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d("CleverPush", "NotificationDismissIntentService: onHandleIntent");


		CleverPush cleverPush = CleverPush.getInstance(this);
		BadgeHelper.update(this, cleverPush.getIncrementBadge());
	}
}
