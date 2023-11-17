package com.cleverpush;

import static com.cleverpush.CleverPush.context;

public interface Constants {
  String LOG_TAG = "CleverPush";
  String DEVICE_ID_CONFIG_FIELD = "preventDuplicateEnabled";
  String APPLICATION_PACKAGE_NAME = context.getApplicationInfo().packageName;
  String APPLICATION_GROUP_NAME = APPLICATION_PACKAGE_NAME.substring(0, APPLICATION_PACKAGE_NAME.lastIndexOf(".") + 1);
  String ACTION_SEND_DEVICE_ID = APPLICATION_GROUP_NAME + ".SEND_DEVICE_ID";
  String ACTION_REQUEST_DEVICE_ID = "com.cleverpush.REQUEST_DEVICE_ID";
  String EXTRA_DEVICE_ID = "deviceId";
  String EXTRA_FULL_PACKAGE_NAME = "fullPackageName";
  Integer COUNTDOWN_TIMER = 1000;
  Integer COUNTDOWN_TIMER_INTERVAL = 1000;
  String GEOFENCE_ENTER_STATE = "enter";
  String GEOFENCE_EXIT_STATE = "exit";
  String CLEVERPUSH_APP_BANNER_UNSUBSCRIBE_EVENT = "CLEVERPUSH_APP_BANNER_UNSUBSCRIBE_EVENT";
  String IABTCF_VendorConsents = "IABTCF_VendorConsents";
}
