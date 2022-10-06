package com.cleverpush.service;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.cleverpush.BadgeHelper;
import com.cleverpush.CleverPush;

public class NotificationDismissIntentService extends IntentService {
	public NotificationDismissIntentService(String name) {
		super(name);
	}

	public NotificationDismissIntentService() {
		super("NotificationIntentService");
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(LOG_TAG, "NotificationDismissIntentService: onHandleIntent");

		CleverPush cleverPush = CleverPush.getInstance(this);
		BadgeHelper.update(this, cleverPush.getIncrementBadge());
	}
}
