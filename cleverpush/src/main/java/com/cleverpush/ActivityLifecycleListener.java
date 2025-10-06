package com.cleverpush;

import static com.cleverpush.Constants.IABTCF_VendorConsents;
import static com.cleverpush.Constants.LOG_TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.SessionListener;
import com.cleverpush.service.CleanUpService;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import java.util.ArrayList;

public class ActivityLifecycleListener implements Application.ActivityLifecycleCallbacks, SharedPreferences.OnSharedPreferenceChangeListener {

  @SuppressLint("StaticFieldLeak")
  public static Activity currentActivity;

  private int counter = 0;
  private static SessionListener sessionListener;
  private static ArrayList<ActivityInitializedListener> activityInitializedListeners = new ArrayList<>();
  private Handler mainHandler = new Handler(Looper.getMainLooper());
  private int activityCount = 0;
  private boolean isInBackground = false;


  public ActivityLifecycleListener(SessionListener sessionListener) {
    this.sessionListener = sessionListener;
  }

  @Nullable
  private static ActivityLifecycleListener instance;

  static void registerActivityLifecycleCallbacks(@NonNull final Application application,
                                                 SessionListener sessionListener) {
    if (instance == null) {
      instance = new ActivityLifecycleListener(sessionListener);
      application.registerActivityLifecycleCallbacks(instance);
    }
  }

  static void registerActivityLifecycleCallbacks(@NonNull final Application application,
                                                 SessionListener sessionListener, Activity activity) {
    registerActivityLifecycleCallbacks(application, sessionListener);
    instance.currentActivity = activity;
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {
    currentActivity = activity;
  }

  @Override
  public void onActivityStarted(Activity activity) {
    activityCount++;
    isInBackground = false;
  }

  @Override
  public void onActivityResumed(Activity activity) {
    Logger.d(LOG_TAG, "onActivityResumed");
    currentActivity = activity;

    if (!isServiceRunning(CleanUpService.class)) {
      try {
        CleverPush.context.startService(new Intent(CleverPush.context, CleanUpService.class));
      } catch (IllegalStateException illegalStateException) {
        Logger.e(LOG_TAG, "Error starting CleanUpService.", illegalStateException);
      }
    }

    if (counter == 0 && sessionListener != null) {
      sessionListener.stateChanged(true);
    }

    try {
      if (activityInitializedListeners != null && activityInitializedListeners.size() > 0) {
        for (ActivityInitializedListener listener : activityInitializedListeners) {
          listener.initialized();
        }
        activityInitializedListeners.clear();
      }
    } catch (Exception error) {
      Logger.e(LOG_TAG, "Error handling activityInitializedListeners - "
          + "activityInitializedListeners != null " + (activityInitializedListeners != null), error);
    }

    counter++;

    try {
      // Register SharedPreferences.OnSharedPreferenceChangeListener
      new Thread(() -> {
        SharedPreferences prefs = SharedPreferencesManager.getSharedPreferences(activity);
        prefs.registerOnSharedPreferenceChangeListener(this);
      }).start();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while registering OnSharedPreferenceChangeListener. " + e.getMessage(), e);
    }
  }

  @Override
  public void onActivityPaused(Activity activity) {
    if (activity == currentActivity) {
      currentActivity = null;
    }

    if (counter > 0) {
      counter--;
    }

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

    CleverPush.getInstance(CleverPush.context).resetInitSessionCalled();

    try {
      // Unregister SharedPreferences.OnSharedPreferenceChangeListener
      new Thread(() -> {
        SharedPreferences prefs = SharedPreferencesManager.getSharedPreferences(activity);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
      }).start();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while unregistering OnSharedPreferenceChangeListener. " + e.getMessage(), e);
    }
  }

  @Override
  public void onActivityStopped(Activity activity) {
    activityCount--;
    if (activityCount == 0) {
      isInBackground = true;
    }
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
    activityInitializedListeners = null;
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

  public void setActivityInitializedListener(ActivityInitializedListener activityInitializedListener) {

    if (currentActivity == null) {
      if (activityInitializedListeners == null) {
        activityInitializedListeners = new ArrayList<>();
      }
      activityInitializedListeners.add(activityInitializedListener);
    } else {
      activityInitializedListener.initialized();
    }
  }

  @Nullable
  public static ActivityLifecycleListener getInstance() {
    return instance;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    mainHandler.post(() -> {
      if (key.equalsIgnoreCase(IABTCF_VendorConsents)) {
        if (CleverPush.getInstance(CleverPush.context).getIabTcfMode() != null && CleverPush.getInstance(CleverPush.context).getIabTcfMode() != IabTcfMode.DISABLED) {
          CleverPush.getInstance(CleverPush.context).setTCF();
        }
      }
    });
  }

  public boolean isAppInBackground() {
    return isInBackground;
  }

  public boolean isAppOpen() {
    return activityCount > 0 && !isInBackground;
  }

}
