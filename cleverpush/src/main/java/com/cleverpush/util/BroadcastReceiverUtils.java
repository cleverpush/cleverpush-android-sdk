package com.cleverpush.util;

import static com.cleverpush.CleverPush.broadcastReceiverHandler;
import static com.cleverpush.CleverPush.context;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Constants;

import java.util.UUID;

public final class BroadcastReceiverUtils {
    /**
     * This function will register broadcast receiver with intent if only channel has preventDuplicateEnabled in config or if it's not already registered
     *
     * @param context
     * @function registerReceiver
     */
    public static void registerReceiver(CleverPush context) {
        if (context.registeredDeviceBroadcastReceiver) {
            return;
        }

        context.getChannelConfig(channelConfig -> {
            if (channelConfig.optString(Constants.DEVICE_ID_CONFIG_FIELD) == null || channelConfig.optBoolean(Constants.DEVICE_ID_CONFIG_FIELD) != true) {
                return;
            }

            context.registeredDeviceBroadcastReceiver = true;
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.DEVICE_ID_ACTION_KEY);
            intentFilter.addAction(Constants.GET_DEVICE_ID_FROM_ALL_DEVICE);
            CleverPush.context.registerReceiver(broadcastReceiverHandler, intentFilter);

            SharedPreferences sharedPreferences = getSharedPreferences(CleverPush.context);
            if (sharedPreferences.getString(CleverPushPreferences.DEVICE_ID, null) == null) {
                String uid = UUID.randomUUID().toString();
                sharedPreferences.edit().putString(CleverPushPreferences.DEVICE_ID, uid).apply();
            }

            getDeviceIdFromOtherApps();
        });
    }

    /**
     * This function will send an event to other apps which inform receivers to send their stored device id if they have any
     *
     * @function getDeviceIdFromOtherApps
     */
    public static void getDeviceIdFromOtherApps() {
        final Intent intent = new Intent();
        intent.setAction(Constants.GET_DEVICE_ID_FROM_ALL_DEVICE);
        intent.putExtra(Constants.GET_FULL_PACKAGE_NAME_KEY, Constants.APPLICATION_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    /**
     * This function will send Device id from shared preference to all other applications
     *
     * @function sendBroadcastMessage
     */
    public static void sendBroadcastMessage() {
        SharedPreferences sharedPreferences = getSharedPreferences(context);
        String deviceId = sharedPreferences.getString(CleverPushPreferences.DEVICE_ID, null);

        final Intent intent = new Intent();
        intent.setAction(Constants.DEVICE_ID_ACTION_KEY);
        intent.putExtra(Constants.DEVICE_ID, deviceId);
        intent.putExtra(Constants.GET_FULL_PACKAGE_NAME_KEY, Constants.APPLICATION_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    /**
     * This function will return Shared preference from preference manager
     *
     * @param context
     * @return SharedPreference
     * @function getSharedPreferences
     */
    public static SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

}
