package com.cleverpush;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.SessionListener;
import com.cleverpush.service.CleanUpService;

public class ActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    @SuppressLint("StaticFieldLeak")
    public static Activity currentActivity;

    private int counter = 0;
    private static SessionListener sessionListener;
    private static ActivityInitializedListener activityInitializedListener;

    public ActivityLifecycleListener(SessionListener sessionListener) {
        this.sessionListener = sessionListener;
    }

    @Nullable
    private static ActivityLifecycleListener instance;

    static void registerActivityLifecycleCallbacks(@NonNull final Application application, SessionListener sessionListener) {
        if (instance == null) {
            instance = new ActivityLifecycleListener(sessionListener);
            application.registerActivityLifecycleCallbacks(instance);
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle bundle) {

    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;

        if (!isServiceRunning(CleanUpService.class)) {
            try {
                CleverPush.context.startService(new Intent(CleverPush.context, CleanUpService.class));
            } catch (IllegalStateException illegalStateException) {
                Log.e("CleverPush", illegalStateException.getMessage());
            }
        }

        if (counter == 0 && sessionListener != null) {
            sessionListener.stateChanged(true);
        }

        activityInitializedListener.initialized();
        counter++;
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity == currentActivity) {
            currentActivity = null;
        }

        counter--;
        if (counter == 0 && sessionListener != null) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (counter == 0 && sessionListener != null) {
                        sessionListener.stateChanged(false);
                    }
                }
            }, 1000);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (activity == currentActivity) {
            currentActivity = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        if (activity == currentActivity) {
            currentActivity = null;
        }
    }

    public static void clearSessionListener() {
        sessionListener = null;
        currentActivity = null;
        activityInitializedListener = null;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) CleverPush.context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void setActivityInitializedListener(ActivityInitializedListener activityInitializedListener) {
        ActivityLifecycleListener.activityInitializedListener = activityInitializedListener;
    }
}
