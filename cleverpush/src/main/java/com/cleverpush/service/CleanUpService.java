package com.cleverpush.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;

public class CleanUpService extends Service {

    private final long EXPECTED_NOTIFICATION_OPENED_ACTIVITY_ON_DESTROY_DELAY = 5000;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);

        boolean shouldStartActivity = CleverPush.getInstance(this).notificationOpenShouldStartActivity();
        long notificationOpenedActivityWasDestroyedAt = CleverPush.getInstance(this).getNotificationOpenedActivityDestroyedAt();
        boolean notificationOpenedActivityWasDestroyedRecently = System.currentTimeMillis() - notificationOpenedActivityWasDestroyedAt < EXPECTED_NOTIFICATION_OPENED_ACTIVITY_ON_DESTROY_DELAY;
        if (shouldStartActivity || !notificationOpenedActivityWasDestroyedRecently){
            CleverPush.removeInstance();
        }

        ActivityLifecycleListener.clearSessionListener();
        this.stopSelf();
    }
}
