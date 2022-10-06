package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DeviceIdBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        getActionName(Constants.APPLICATION_GROUP_NAME);
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        if (intent.getAction().equals(getActionName(Constants.APPLICATION_GROUP_NAME)) && intent.hasExtra(Constants.DEVICE_ID)) {
            sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, intent.getStringExtra(Constants.DEVICE_ID)).apply();
        }
    }

    private String getActionName(String applicationGroupName) {
        String[] parts = applicationGroupName.split("\\.");
        String splitedString = parts[0].concat(".").concat(parts[1]).concat(".").concat(Constants.DEVICE_ID_INTENT);
        return splitedString;
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
