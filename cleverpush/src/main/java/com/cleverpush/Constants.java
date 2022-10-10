package com.cleverpush;

import static com.cleverpush.CleverPush.context;

public interface Constants {
    String LOG_TAG = "CleverPush";
    String DEVICE_ID = "deviceId";
    String DEVICE_ID_CONFIG_FIELD = "preventDuplicatePushesEnabled";
    String DEVICE_ID_INTENT = "device_id";
    String APPLICATION_PACKAGE_NAME = context.getApplicationInfo().packageName;
    String APPLICATION_GROUP_NAME = APPLICATION_PACKAGE_NAME.substring(0, APPLICATION_PACKAGE_NAME.lastIndexOf(".") + 1);
    String DEVICE_ID_ACTION_KEY = APPLICATION_GROUP_NAME.concat(Constants.DEVICE_ID_INTENT);
    String GET_DEVICE_ID_FROM_ALL_DEVICE = "getDeviceIdFromAllDevice";
    String GET_FULL_PACKAGE_NAME_KEY = "getFullPackageNameKey";
}
