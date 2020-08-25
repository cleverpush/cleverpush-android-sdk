package com.cleverpush.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;

import androidx.core.app.JobIntentService;

import com.cleverpush.Notification;
import com.cleverpush.Subscription;

import java.util.List;

public abstract class NotificationExtenderService extends JobIntentService {

	static final int EXTENDER_SERVICE_JOB_ID = 209538983;

	protected abstract boolean onNotificationProcessing(Notification notification);

	@Override
	protected final void onHandleWork(Intent intent) {
		if (intent == null) {
			return;
		}

		Bundle bundle = intent.getExtras();

		if (bundle == null) {
			return;
		}

		Notification notification = (Notification) bundle.getSerializable("notification");
		Subscription subscription = (Subscription) bundle.getSerializable("subscription");
		if (notification == null || subscription == null) {
			Log.e("CleverPush", "notification extra is missing in NotificationExtenderService: " + bundle);
			return;
		}

		boolean developerProcessed = false;
		try {
			developerProcessed = onNotificationProcessing(notification);
		} catch (Throwable t) {
			Log.e("CleverPush", "onNotificationProcessing exception", t);
		}

		if (!developerProcessed) {
			NotificationService.getInstance().showNotification(this, notification, subscription);
		}
	}

	static Intent getIntent(Context context) {
		PackageManager packageManager = context.getPackageManager();
		Intent intent = new Intent().setAction("com.cleverpush.service.NotificationExtender").setPackage(context.getPackageName());
		List<ResolveInfo> resolveInfo = packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA);
		if (resolveInfo.size() < 1) {
			return null;
		}
		intent.setComponent(new ComponentName(context,resolveInfo.get(0).serviceInfo.name));
		return intent;
	}
}
