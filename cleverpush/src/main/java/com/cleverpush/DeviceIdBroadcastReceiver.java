package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.UUID;

public class DeviceIdBroadcastReceiver extends BroadcastReceiver {
    String device_id;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
//        Sender
        if (intent.getAction().equals("com.cleverpush")) {
            if (intent.hasExtra("deviceId")) {
                sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, intent.getStringExtra("deviceId")).apply();
            }
            device_id = sharedPreferences.getString(CleverPushPreferences.DEVICE_ID, null);
        } else {
            sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, UUID.randomUUID().toString()).apply();
            device_id = UUID.randomUUID().toString();
        }
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

}
