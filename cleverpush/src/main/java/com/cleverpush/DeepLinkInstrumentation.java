package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PersistableBundle;

import com.cleverpush.util.Logger;

import java.lang.reflect.Field;

/**
 * Process-wide {@link Instrumentation} wrapper so {@code onNewIntent} is visible for every
 * host {@link Activity}, including raw Activity (singleTop / singleTask) reuse.
 */
final class DeepLinkInstrumentation extends Instrumentation {

  private static final Object INSTALL_LOCK = new Object();
  private static boolean installed;
  private static boolean installBlocked;

  private final Instrumentation base;

  DeepLinkInstrumentation(Instrumentation base) {
    this.base = base != null ? base : new Instrumentation();
  }

  @SuppressLint("PrivateApi")
  static void install() {
    synchronized (INSTALL_LOCK) {
      if (installed || installBlocked) {
        return;
      }
      try {
        Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
        Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
        if (activityThread == null) {
          return;
        }
        Field field = activityThreadClass.getDeclaredField("mInstrumentation");
        field.setAccessible(true);
        Instrumentation current = (Instrumentation) field.get(activityThread);
        if (current instanceof DeepLinkInstrumentation) {
          installed = true;
          return;
        }
        field.set(activityThread, new DeepLinkInstrumentation(current));
        installed = true;
      } catch (Throwable throwable) {
        installBlocked = true;
        Logger.d(LOG_TAG, "DeepLinkTracker: process instrumentation unavailable; "
            + "raw Activity hosts should call CleverPush.onNewIntent(activity, intent)");
      }
    }
  }

  @Override
  public void callActivityOnNewIntent(Activity activity, Intent intent) {
    base.callActivityOnNewIntent(activity, intent);
    DeepLinkTracker.captureFromNewIntent(activity, intent);
  }

  @Override
  public Activity newActivity(ClassLoader cl, String className, Intent intent)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return base.newActivity(cl, className, intent);
  }

  @Override
  public Activity newActivity(Class<?> clazz, Context context, IBinder token,
      Application application, Intent intent, ActivityInfo info, CharSequence title,
      Activity parent, String id, Object lastNonConfigurationInstance)
      throws InstantiationException, IllegalAccessException {
    return base.newActivity(clazz, context, token, application, intent, info, title, parent,
        id, lastNonConfigurationInstance);
  }

  @Override
  public Application newApplication(ClassLoader cl, String className, Context context)
      throws InstantiationException, IllegalAccessException, ClassNotFoundException {
    return base.newApplication(cl, className, context);
  }

  @Override
  public void callApplicationOnCreate(Application app) {
    base.callApplicationOnCreate(app);
  }

  @Override
  public void callActivityOnCreate(Activity activity, Bundle icicle) {
    base.callActivityOnCreate(activity, icicle);
  }

  @Override
  public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
    base.callActivityOnCreate(activity, icicle, persistentState);
  }

  @Override
  public void callActivityOnDestroy(Activity activity) {
    base.callActivityOnDestroy(activity);
  }

  @Override
  public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState) {
    base.callActivityOnRestoreInstanceState(activity, savedInstanceState);
  }

  @Override
  public void callActivityOnRestoreInstanceState(Activity activity, Bundle savedInstanceState,
      PersistableBundle persistentState) {
    base.callActivityOnRestoreInstanceState(activity, savedInstanceState, persistentState);
  }

  @Override
  public void callActivityOnPostCreate(Activity activity, Bundle savedInstanceState) {
    base.callActivityOnPostCreate(activity, savedInstanceState);
  }

  @Override
  public void callActivityOnPostCreate(Activity activity, Bundle savedInstanceState,
      PersistableBundle persistentState) {
    base.callActivityOnPostCreate(activity, savedInstanceState, persistentState);
  }

  @Override
  public void callActivityOnStart(Activity activity) {
    base.callActivityOnStart(activity);
  }

  @Override
  public void callActivityOnRestart(Activity activity) {
    base.callActivityOnRestart(activity);
  }

  @Override
  public void callActivityOnResume(Activity activity) {
    base.callActivityOnResume(activity);
  }

  @Override
  public void callActivityOnStop(Activity activity) {
    base.callActivityOnStop(activity);
  }

  @Override
  public void callActivityOnSaveInstanceState(Activity activity, Bundle outState) {
    base.callActivityOnSaveInstanceState(activity, outState);
  }

  @Override
  public void callActivityOnSaveInstanceState(Activity activity, Bundle outState,
      PersistableBundle persistentState) {
    base.callActivityOnSaveInstanceState(activity, outState, persistentState);
  }

  @Override
  public void callActivityOnPause(Activity activity) {
    base.callActivityOnPause(activity);
  }

  @Override
  public void callActivityOnUserLeaving(Activity activity) {
    base.callActivityOnUserLeaving(activity);
  }

  @Override
  public void callActivityOnPictureInPictureRequested(Activity activity) {
    base.callActivityOnPictureInPictureRequested(activity);
  }

  @Override
  public boolean onException(Object obj, Throwable e) {
    return base.onException(obj, e);
  }
}
