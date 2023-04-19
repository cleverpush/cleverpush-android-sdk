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
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
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
    SharedPreferences sharedPreferences = getSharedPreferences(context);
    sharedPreferences.edit().putString(key, value).apply();
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
    SharedPreferences sharedPreferences = getSharedPreferences(context);
    sharedPreferences.edit().putBoolean(key, value).apply();
  }
}
