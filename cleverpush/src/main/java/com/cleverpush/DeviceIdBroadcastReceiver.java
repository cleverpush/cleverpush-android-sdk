package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

public class DeviceIdBroadcastReceiver extends BroadcastReceiver {
    String deviceId;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (intent.getAction().equals(Constants.SDK_PKG)) {
            if (intent.hasExtra(Constants.DEVICE_ID_INTENT)) {
                sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, intent.getStringExtra(Constants.DEVICE_ID_INTENT)).apply();
            }
            deviceId = sharedPreferences.getString(CleverPushPreferences.DEVICE_ID, null);
        } else {
            sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, UUID.randomUUID().toString()).apply();
            deviceId = UUID.randomUUID().toString();
        }
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

}
