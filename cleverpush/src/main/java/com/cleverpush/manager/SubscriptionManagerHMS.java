package com.cleverpush.manager;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.hms.aaid.HmsInstanceId;
import com.huawei.hms.common.ApiException;
import com.huawei.hms.push.HmsMessaging;

public class SubscriptionManagerHMS extends SubscriptionManagerBase {

    private static final int TOKEN_TIMEOUT_MS = 30_000;

    public SubscriptionManagerHMS(Context context) {
        super(context);
    }

    @Override
    public String getProviderName() {
        return "HMS";
    }

    private static boolean callbackSuccessful;
    private @Nullable static RegisteredHandler registeredHandler;

    @Override
    public void subscribe(RegisteredHandler callback) {
        registeredHandler = callback;
        new Thread(() -> {
            try {
                getHMSTokenTask(context, callback);
            } catch (ApiException e) {
                Log.e("CleverPush", "HMS ApiException getting Huawei token", e);

                callback.complete(null);
            }
        }, "HMS_GET_TOKEN").start();
    }

    public static void fireCallback(String token) {
        if (registeredHandler == null) {
            return;
        }
        callbackSuccessful = true;
        registeredHandler.complete(token);
    }

    private synchronized void getHMSTokenTask(@NonNull Context context, @NonNull RegisteredHandler callback) throws ApiException {
        String appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
        HmsInstanceId hmsInstanceId = HmsInstanceId.getInstance(context);

        String pushToken = hmsInstanceId.getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE);

        if (!TextUtils.isEmpty(pushToken)) {
            Log.d("CleverPush", "Device registered for HMS, push token = " + pushToken);
            callback.complete(pushToken);
        } else {
            // Token is always null on Huawei EMUI <= 9. We need to wait for the event.
            waitForOnNewPushTokenEvent();
        }
    }

    private static void waitForOnNewPushTokenEvent() {
        try {
            Thread.sleep(TOKEN_TIMEOUT_MS);
        } catch (InterruptedException e) {
        }

        if (!callbackSuccessful) {
            Log.e("CleverPush", "SubscriptionManagerHMS onNewToken timeout");
            registeredHandler.complete(null);
        }
    }
}
