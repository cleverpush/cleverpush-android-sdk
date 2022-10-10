package com.cleverpush;

import static com.cleverpush.CleverPush.context;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.util.Logger;

public class DeviceIdBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getStringExtra(Constants.GET_FULL_PACKAGE_NAME_KEY).equals(Constants.APPLICATION_PACKAGE_NAME)) {
            return;
        }

        if (intent.getAction().equals(Constants.GET_DEVICE_ID_FROM_ALL_DEVICE)) {
            sendBrodCastReceiver();
        }

        if (intent.getAction().equals(Constants.DEVICE_ID_ACTION_KEY) && intent.hasExtra(Constants.DEVICE_ID)) {
            SharedPreferences sharedPreferences = getSharedPreferences(context);
            String deviceId = intent.getStringExtra(Constants.DEVICE_ID);
            sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, deviceId).apply();
        }

    }

    private void sendBrodCastReceiver() {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String deviceId = sharedPreferences.getString(CleverPushPreferences.DEVICE_ID, null);
        final Intent intent = new Intent();
        intent.putExtra(Constants.DEVICE_ID, deviceId);
        intent.putExtra(Constants.GET_FULL_PACKAGE_NAME_KEY, Constants.APPLICATION_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
