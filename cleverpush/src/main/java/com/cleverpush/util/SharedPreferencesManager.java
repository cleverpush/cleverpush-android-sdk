package com.cleverpush.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Map;
import java.util.Set;

public class SharedPreferencesManager {

  public static final String SDK_PREFERENCES_NAME = "CleverPush";
  private SharedPreferences sharedPreferences;
  private SharedPreferences.Editor editor;
  private Context context;

  public SharedPreferencesManager(Context context) {
    this.context = context;
    sharedPreferences = context.getSharedPreferences(SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);
    editor = sharedPreferences.edit();
  }

  public static SharedPreferences getSharedPreferences(Context context) {
    SharedPreferences sharedPreferences = context.getSharedPreferences(SharedPreferencesManager.SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);
    return sharedPreferences;
  }

  /*
   * Migrate default shared preferences to CleverPush shared preferences
   * This method copies all key-value pairs from the default shared preferences to CleverPush shared preferences.
   * */
  public static void migrateSharedPreferences(Context context) {
    try {
      SharedPreferences defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);
      SharedPreferences sdkPreferences = context.getSharedPreferences(SDK_PREFERENCES_NAME, Context.MODE_PRIVATE);

      // Start migration
      SharedPreferences.Editor editor = sdkPreferences.edit();
      Map<String, ?> allEntries = defaultPreferences.getAll();
      for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
        String key = entry.getKey();
        Object value = entry.getValue();

        // Handle different data types
        if (value instanceof String) {
          editor.putString(key, (String) value);
        } else if (value instanceof Boolean) {
          editor.putBoolean(key, (Boolean) value);
        } else if (value instanceof Integer) {
          editor.putInt(key, (Integer) value);
        } else if (value instanceof Float) {
          editor.putFloat(key, (Float) value);
        } else if (value instanceof Long) {
          editor.putLong(key, (Long) value);
        } else if (value instanceof Set<?>) {
          Set<String> stringSet = (Set<String>) value;
          editor.putStringSet(key, stringSet);
        }
        // Add handling for other data types if necessary
      }
      editor.apply();
    } catch (Exception e) {
      Logger.e("CleverPush", "Error while migrating default preference into CleverPush preference. " + e.getLocalizedMessage(), e);
    }
  }

  public void setString(String key, String value) {
    editor.putString(key, value);
    editor.apply();
  }

  public String getString(String key, String defaultValue) {
    return sharedPreferences.getString(key, defaultValue);
  }

  public void setInt(String key, int value) {
    editor.putInt(key, value);
    editor.apply();
  }

  public int getInt(String key, int defaultValue) {
    return sharedPreferences.getInt(key, defaultValue);
  }

  public void setBoolean(String key, boolean value) {
    editor.putBoolean(key, value);
    editor.apply();
  }

  public boolean getBoolean(String key, boolean defaultValue) {
    return sharedPreferences.getBoolean(key, defaultValue);
  }

}
