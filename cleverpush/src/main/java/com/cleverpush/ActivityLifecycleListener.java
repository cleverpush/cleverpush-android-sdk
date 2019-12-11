package com.cleverpush;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

class ActivityLifecycleListener implements Application.ActivityLifecycleCallbacks {

    @SuppressLint("StaticFieldLeak")
    static Activity currentActivity;

    @Nullable
    private static ActivityLifecycleListener instance;

    static void registerActivityLifecycleCallbacks(@NonNull final Application application) {
        if (instance == null) {
            instance = new ActivityLifecycleListener();
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
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (activity == currentActivity) {
            currentActivity = null;
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
}
