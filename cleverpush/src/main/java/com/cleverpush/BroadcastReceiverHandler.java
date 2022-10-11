package com.cleverpush;

import static com.cleverpush.util.BroadcastReceiverUtils.sendBroadcastMessage;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class BroadcastReceiverHandler extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (intent.getStringExtra(Constants.GET_FULL_PACKAGE_NAME_KEY).equals(Constants.APPLICATION_PACKAGE_NAME)) {
            return;
        }

        if (action.equals(Constants.GET_DEVICE_ID_FROM_ALL_DEVICE)) {
            sendBroadcastMessage(this);
            return;
        }

        if (action.equals(Constants.DEVICE_ID_ACTION_KEY) && intent.hasExtra(Constants.DEVICE_ID)) {
            SharedPreferences sharedPreferences = getSharedPreferences(context);
            String deviceId = intent.getStringExtra(Constants.DEVICE_ID);
            sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, deviceId).apply();
        }
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
