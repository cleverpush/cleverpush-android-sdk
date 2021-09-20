package com.cleverpush;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cleverpush.listener.SessionListener;

public class ActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    @SuppressLint("StaticFieldLeak")
    public static Activity currentActivity;

    private int counter = 0;
    private static SessionListener sessionListener;

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

        if (counter == 0 && sessionListener != null) {
            sessionListener.stateChanged(true);
        }
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
        sessionListener = null;
        CleverPush.removeInstance();
    }

    public static void clearSessionListner(){
        sessionListener = null;
        currentActivity = null;
    }
}
