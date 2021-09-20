package com.cleverpush.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;

public class CleanUpService extends Service {


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
        CleverPush.removeInstance();
        ActivityLifecycleListener.clearSessionListner();
        this.stopSelf();
    }
}