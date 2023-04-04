package com.cleverpush;

import static com.cleverpush.util.BroadcastReceiverUtils.sendDeviceId;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cleverpush.util.PreferenceManagerUtils;

public class BroadcastReceiverHandler extends BroadcastReceiver {
  @Override
  public void onReceive(Context context, Intent intent) {
    String action = intent.getAction();

    if (intent.hasExtra(Constants.EXTRA_FULL_PACKAGE_NAME)
        && intent.getStringExtra(Constants.EXTRA_FULL_PACKAGE_NAME).equals(Constants.APPLICATION_PACKAGE_NAME)) {
      return;
    }

    if (action == null) {
      return;
    }

    if (action.equals(Constants.ACTION_REQUEST_DEVICE_ID)) {
      sendDeviceId();
      return;
    }

    if (action.equals(Constants.ACTION_SEND_DEVICE_ID) && intent.hasExtra(Constants.EXTRA_DEVICE_ID)) {
      String deviceId = intent.getStringExtra(Constants.EXTRA_DEVICE_ID);
      PreferenceManagerUtils.updateSharedPreferenceByKey(context, CleverPushPreferences.DEVICE_ID, deviceId);
    }
  }
}
