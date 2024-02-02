package com.cleverpush;

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

public class SharedPreferencesLiveData extends LiveData<String> implements SharedPreferences.OnSharedPreferenceChangeListener {

  private final SharedPreferences sharedPreferences;
  private String key;

  public SharedPreferencesLiveData(SharedPreferences sharedPreferences, String key) {
    this.sharedPreferences = sharedPreferences;
    this.key = key;
    setValue(sharedPreferences.getString(key, "0"));
  }

  @Override
  protected void onActive() {
    super.onActive();
    sharedPreferences.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onInactive() {
    super.onInactive();
    sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (this.key.equals(key)) {
      setValue(sharedPreferences.getString(key, "0"));
    }
  }
}
