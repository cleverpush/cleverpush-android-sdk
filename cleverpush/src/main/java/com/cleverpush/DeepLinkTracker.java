package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;

import com.cleverpush.banner.WebViewActivity;
import com.cleverpush.inbox.InboxDetailActivity;
import com.cleverpush.stories.StoryDetailActivity;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.WeakHashMap;

/**
 * Detects deep links opened in the host app, stores the URL, and attributes it on
 * later {@code trackEvent} calls. Does not send events on its own.
 * <p>
 * Reused host activities of any {@link Activity} subclass are handled automatically.
 * New-intent delivery is intercepted process-wide so a stale {@link Activity#getIntent()}
 * does not drop later deep links. ComponentActivity listeners are a fallback.
 */
public final class DeepLinkTracker {

  static final String INTENT_EXTRA_TRACKED = "cleverpush_deep_link_tracked";

  private static final String DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
  private static final long ATTRIBUTION_WINDOW_MS = 24L * 60L * 60L * 1000L;

  private static String lastProcessedUrl;
  private static int lastProcessedIntentHash;
  private static final Object NEW_INTENT_LOCK = new Object();
  private static final WeakHashMap<Activity, Boolean> newIntentCaptureRegistered = new WeakHashMap<>();
  private static final WeakHashMap<Activity, Intent> latestNewIntents = new WeakHashMap<>();

  private DeepLinkTracker() {
  }

  public static void captureFromActivity(Activity activity) {
    if (shouldIgnoreActivity(activity)) {
      return;
    }
    ensureOnNewIntentCapture(activity);
    captureFromIntent(resolveCaptureIntent(activity), activity.getApplicationContext());
  }

  /**
   * Called automatically when a reused host activity receives a new deep link.
   * Forwards that intent to {@link #captureFromIntent} and updates the activity
   * intent so later lifecycle captures do not keep attributing the original URL.
   */
  static void captureFromNewIntent(Activity activity, Intent intent) {
    if (shouldIgnoreActivity(activity) || intent == null) {
      return;
    }
    synchronized (NEW_INTENT_LOCK) {
      latestNewIntents.put(activity, intent);
    }
    try {
      activity.setIntent(intent);
    } catch (Exception ignored) {
      // Host activity may not allow intent replacement; capture still uses the new intent.
    }
    captureFromIntent(intent, activity.getApplicationContext());
  }

  public static void captureFromIntent(Intent intent, Context context) {
    if (intent == null || context == null) {
      return;
    }

    if (intent.getBooleanExtra(INTENT_EXTRA_TRACKED, false)) {
      return;
    }

    String action = intent.getAction();
    if (Intent.ACTION_MAIN.equals(action)) {
      return;
    }

    String url = extractDeepLinkUrl(intent, context);
    if (url == null) {
      return;
    }

    int intentHash = System.identityHashCode(intent);
    if (url.equals(lastProcessedUrl) && intentHash == lastProcessedIntentHash) {
      return;
    }

    storeDeepLink(context, url);
    lastProcessedUrl = url;
    lastProcessedIntentHash = intentHash;
    try {
      intent.putExtra(INTENT_EXTRA_TRACKED, true);
    } catch (Exception ignored) {
      // Some intents do not allow extra mutation; storage still succeeded.
    }
  }

  static void storeDeepLink(Context context, String url) {
    if (context == null) {
      return;
    }

    String normalizedUrl = normalizeDeepLinkUrl(url);
    if (normalizedUrl == null) {
      return;
    }

    try {
      SharedPreferences.Editor editor = SharedPreferencesManager.getSharedPreferences(context).edit();
      editor.putString(CleverPushPreferences.LAST_DEEP_LINK_URL, normalizedUrl);
      editor.putString(CleverPushPreferences.LAST_DEEP_LINK_TIME, getCurrentDateTime());
      editor.apply();
      Logger.d(LOG_TAG, "DeepLinkTracker: stored deep link " + normalizedUrl);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "DeepLinkTracker: failed to store deep link", e);
    }
  }

  static void addAttributionToEvent(JSONObject jsonBody, SharedPreferences sharedPreferences) {
    if (jsonBody == null || sharedPreferences == null) {
      return;
    }

    try {
      String lastDeepLinkUrl = sharedPreferences.getString(CleverPushPreferences.LAST_DEEP_LINK_URL, null);
      String lastDeepLinkTime = sharedPreferences.getString(CleverPushPreferences.LAST_DEEP_LINK_TIME, null);
      if (lastDeepLinkUrl != null && !lastDeepLinkUrl.isEmpty() && isWithinAttributionWindow(lastDeepLinkTime)) {
        jsonBody.put("deeplinkId", lastDeepLinkUrl);
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "DeepLinkTracker: failed to add deep link attribution", e);
    }
  }

  static String extractDeepLinkUrl(Intent intent, Context context) {
    if (intent == null) {
      return null;
    }

    Uri data = intent.getData();
    if (data != null) {
      String url = data.toString();
      if (isTrackableDeepLink(url) && DeepLinkAllowlist.allows(context, url)) {
        return url;
      }
    }

    String dataString = intent.getDataString();
    if (isTrackableDeepLink(dataString) && DeepLinkAllowlist.allows(context, dataString)) {
      return dataString;
    }

    return null;
  }

  static String normalizeDeepLinkUrl(String url) {
    if (url == null) {
      return null;
    }
    String trimmed = url.trim();
    if (!isTrackableDeepLink(trimmed)) {
      return null;
    }
    return trimmed;
  }

  static boolean isTrackableDeepLink(String url) {
    if (url == null || url.trim().isEmpty()) {
      return false;
    }

    try {
      URI uri = new URI(url.trim());
      String scheme = uri.getScheme();
      if (scheme == null || scheme.isEmpty()) {
        return false;
      }

      scheme = scheme.toLowerCase(Locale.US);
      if (scheme.equals("javascript") || scheme.equals("file") || scheme.equals("content")
          || scheme.equals("data") || scheme.equals("about") || scheme.equals("mailto")
          || scheme.equals("tel") || scheme.equals("sms")) {
        return false;
      }

      if (scheme.equals("http") || scheme.equals("https")) {
        return uri.getHost() != null && !uri.getHost().isEmpty();
      }

      String schemeSpecificPart = uri.getSchemeSpecificPart();
      return schemeSpecificPart != null && !schemeSpecificPart.isEmpty();
    } catch (Exception e) {
      return false;
    }
  }

  static boolean isWithinAttributionWindow(String lastDeepLinkTime) {
    if (lastDeepLinkTime == null || lastDeepLinkTime.isEmpty()) {
      return false;
    }
    try {
      SimpleDateFormat sdf = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.US);
      Date lastOpenedTime = sdf.parse(lastDeepLinkTime);
      if (lastOpenedTime == null) {
        return false;
      }
      long diffInMilliseconds = Math.abs(Calendar.getInstance().getTime().getTime() - lastOpenedTime.getTime());
      return diffInMilliseconds < ATTRIBUTION_WINDOW_MS;
    } catch (Exception e) {
      Logger.e(LOG_TAG, "DeepLinkTracker: error parsing deep link time", e);
      return false;
    }
  }

  static boolean shouldIgnoreActivity(Activity activity) {
    if (activity == null) {
      return true;
    }
    return activity instanceof PermissionActivity
        || activity instanceof NotificationOpenedActivity
        || activity instanceof WebViewActivity
        || activity instanceof InboxDetailActivity
        || activity instanceof StoryDetailActivity;
  }

  static void ensureOnNewIntentCapture(Activity activity) {
    if (activity == null) {
      return;
    }
    synchronized (NEW_INTENT_LOCK) {
      if (newIntentCaptureRegistered.containsKey(activity)) {
        return;
      }
      newIntentCaptureRegistered.put(activity, Boolean.TRUE);
    }
    registerOnNewIntentCapture(activity);
  }

  static void clearActivity(Activity activity) {
    if (activity == null) {
      return;
    }
    synchronized (NEW_INTENT_LOCK) {
      newIntentCaptureRegistered.remove(activity);
      latestNewIntents.remove(activity);
    }
  }

  private static Intent resolveCaptureIntent(Activity activity) {
    synchronized (NEW_INTENT_LOCK) {
      Intent latestNewIntent = latestNewIntents.get(activity);
      if (latestNewIntent != null) {
        return latestNewIntent;
      }
    }
    return activity.getIntent();
  }

  /**
   * Registers new-intent capture for every host {@link Activity}. Process instrumentation
   * covers raw Activity hosts; ComponentActivity listeners remain as a fallback.
   */
  private static void registerOnNewIntentCapture(Activity activity) {
    DeepLinkInstrumentation.install();
    registerComponentActivityListener(activity);
  }

  private static void registerComponentActivityListener(Activity activity) {
    try {
      Class<?> componentActivityClass = Class.forName("androidx.activity.ComponentActivity");
      if (!componentActivityClass.isInstance(activity)) {
        return;
      }
      Method addOnNewIntentListener = findAddOnNewIntentListener(componentActivityClass);
      if (addOnNewIntentListener == null) {
        return;
      }
      Class<?> listenerType = addOnNewIntentListener.getParameterTypes()[0];
      Object listener = java.lang.reflect.Proxy.newProxyInstance(
          listenerType.getClassLoader(),
          new Class<?>[] {listenerType},
          (proxy, method, args) -> {
            if (args != null && args.length == 1 && args[0] instanceof Intent) {
              captureFromNewIntent(activity, (Intent) args[0]);
            }
            return null;
          });
      addOnNewIntentListener.invoke(activity, listener);
    } catch (Throwable ignored) {
      // Not a ComponentActivity, or addOnNewIntentListener is unavailable.
    }
  }

  private static Method findAddOnNewIntentListener(Class<?> componentActivityClass) {
    for (Method method : componentActivityClass.getMethods()) {
      if ("addOnNewIntentListener".equals(method.getName())
          && method.getParameterTypes().length == 1) {
        return method;
      }
    }
    return null;
  }

  private static String getCurrentDateTime() {
    try {
      Date time = Calendar.getInstance().getTime();
      SimpleDateFormat outputFmt = new SimpleDateFormat(DATE_TIME_FORMAT, Locale.US);
      return outputFmt.format(time);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "DeepLinkTracker: error while getting current date and time", e);
      return "";
    }
  }
}
