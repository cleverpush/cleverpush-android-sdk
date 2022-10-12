package com.cleverpush.util;

import static com.cleverpush.CleverPush.broadcastReceiverHandler;
import static com.cleverpush.CleverPush.context;

import android.content.Intent;
import android.content.IntentFilter;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Constants;

import java.util.UUID;

public final class BroadcastReceiverUtils {
    private static boolean registeredDeviceIdBroadcastReceiver = false;

    /**
     * This function will register broadcast receiver with intent if only channel has preventDuplicateEnabled in config or if it's not already registered
     *
     * @param cleverPushInstance
     * @function registerReceiver
     */
    public static void registerReceiver(CleverPush cleverPushInstance) {
        if (registeredDeviceIdBroadcastReceiver) {
            return;
        }
        registeredDeviceIdBroadcastReceiver = true;

        cleverPushInstance.getChannelConfig(channelConfig -> {
            if (channelConfig == null || !channelConfig.optBoolean(Constants.DEVICE_ID_CONFIG_FIELD)) {
                return;
            }

            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(Constants.ACTION_SEND_DEVICE_ID);
            intentFilter.addAction(Constants.ACTION_REQUEST_DEVICE_ID);
            CleverPush.context.registerReceiver(broadcastReceiverHandler, intentFilter);

            if (PreferenceManagerUtils.getSharedPreferenceByKey(context, CleverPushPreferences.DEVICE_ID) == null) {
                String uid = UUID.randomUUID().toString();
                PreferenceManagerUtils.updateSharedPreferenceByKey(context, CleverPushPreferences.DEVICE_ID, uid);
            }
            requestDeviceId();
        });
    }

    /**
     * This function will send an event to other apps which inform receivers to send their stored device id if they have any
     *
     * @function getDeviceIdFromOtherApps
     */
    public static void requestDeviceId() {
        final Intent intent = new Intent();
        intent.setAction(Constants.ACTION_REQUEST_DEVICE_ID);
        intent.putExtra(Constants.EXTRA_FULL_PACKAGE_NAME, Constants.APPLICATION_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }

    /**
     * This function will send Device id from shared preference to all other applications
     *
     * @function sendBroadcastMessage
     */
    public static void sendDeviceId() {
        String deviceId = PreferenceManagerUtils.getSharedPreferenceByKey(context, CleverPushPreferences.DEVICE_ID);

        final Intent intent = new Intent();
        intent.setAction(Constants.ACTION_SEND_DEVICE_ID);
        intent.putExtra(Constants.EXTRA_DEVICE_ID, deviceId);
        intent.putExtra(Constants.EXTRA_FULL_PACKAGE_NAME, Constants.APPLICATION_PACKAGE_NAME);
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        context.sendBroadcast(intent);
    }
}
