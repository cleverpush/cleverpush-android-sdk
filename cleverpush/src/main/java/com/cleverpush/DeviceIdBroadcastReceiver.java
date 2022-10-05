package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DeviceIdBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (intent.getAction().equals(Constants.DEVICE_ID_INTENT) && intent.hasExtra(Constants.DEVICE_ID)) {
            sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, intent.getStringExtra(Constants.DEVICE_ID)).apply();
        }
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
