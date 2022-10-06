package com.cleverpush;

import static com.cleverpush.CleverPush.context;

public interface Constants {
    String LOG_TAG = "CleverPush";
    String DEVICE_ID = "deviceId";
    String DEVICE_ID_CONFIG_FIELD = "preventDuplicatePushesEnabled";
    String DEVICE_ID_INTENT = "device_id";
    String APPLICATION_GROUP_NAME = context.getApplicationInfo().packageName;
}
