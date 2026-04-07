package com.cleverpush.service;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;

public class CleanUpService extends Service {

  private static final long EXPECTED_NOTIFICATION_OPENED_ACTIVITY_ON_DESTROY_DELAY = 5000;
  private static final long CLEANUP_DELAY = 5000;

  @Nullable
  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }

  @Override
  public void onTaskRemoved(Intent rootIntent) {
    super.onTaskRemoved(rootIntent);

    ActivityLifecycleListener lifecycleListener = ActivityLifecycleListener.getInstance();

    // Lifecycle not registered yet (e.g. cold start) — skip cleanup.
    if (lifecycleListener == null) {
      stopSelf();
      return;
    }

    boolean isAppOpen = lifecycleListener.isAppOpen();
    boolean isAppInBackground = lifecycleListener.isAppInBackground();

    // App may still be foreground or not marked background — skip cleanup.
    if (isAppOpen || !isAppInBackground) {
      stopSelf();
      return;
    }

    CleverPush cleverPush = CleverPush.getInstance(this);

    long destroyedAt = cleverPush.getNotificationOpenedActivityDestroyedAt();
    boolean destroyedRecently =
        System.currentTimeMillis() - destroyedAt
            < EXPECTED_NOTIFICATION_OPENED_ACTIVITY_ON_DESTROY_DELAY;

    // Skip while notification tap is being processed or NotificationOpenedActivity just finished.
    if (cleverPush.isNotificationClickInProgress() || destroyedRecently) {
      stopSelf();
      return;
    }

    // Delay cleanup to avoid racing task removal vs. activity lifecycle.
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      ActivityLifecycleListener listener = ActivityLifecycleListener.getInstance();
      if (listener == null || listener.isAppOpen()) {
        return;
      }
      
      if (CleverPush.isNotificationClickInProgress()) {
        return;
      }
      // Only clear lifecycle session state if the singleton was actually removed; otherwise we would
      // leave CleverPush initialized but with a null SessionListener (breaks sync / session).
      if (CleverPush.removeInstance()) {
        ActivityLifecycleListener.clearSessionListener();
      }
    }, CLEANUP_DELAY);

    stopSelf();
  }
}
