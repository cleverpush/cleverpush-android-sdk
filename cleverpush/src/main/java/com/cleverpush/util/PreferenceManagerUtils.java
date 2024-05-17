package com.cleverpush.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public final class PreferenceManagerUtils {
  /**
   * This function will return Shared preference from preference manager
   *
   * @param context
   * @return SharedPreferences
   * @function getSharedPreferences
   */
  @Deprecated
  public static SharedPreferences getSharedPreferences(Context context) {
    return PreferenceManager.getDefaultSharedPreferences(context);
  }

  /**
   * This function will return value from shared preference based on passed key
   *
   * @param context
   * @param key
   * @return String
   * @function getSharedPreferenceByKey
   */
  public static String getSharedPreferenceByKey(Context context, String key) {
    SharedPreferencesManager sharedPreferences = new SharedPreferencesManager(context);
    return sharedPreferences.getString(key, null);
  }

  /**
   * This function will update your shared preference based on key and value
   *
   * @param context
   * @param key
   * @param value
   * @function updateSharedPreferenceByKey
   */
  public static void updateSharedPreferenceByKey(Context context, String key, String value) {
    SharedPreferencesManager sharedPreferences = new SharedPreferencesManager(context);
    sharedPreferences.setString(key, value);
  }

  /**
   * This function will update your shared preference based on key and value
   *
   * @param context
   * @param key
   * @param value
   * @function updateSharedPreferenceByKey
   */
  public static void updateSharedPreferenceByKey(Context context, String key, boolean value) {
    SharedPreferencesManager sharedPreferences = new SharedPreferencesManager(context);
    sharedPreferences.setBoolean(key, value);
  }
}
