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
    private String appId;

    public SubscriptionManagerHMS(Context context) {
        super(context);

        appId = AGConnectServicesConfig.fromContext(context).getString("client/app_id");
    }

    @Override
    public String getProviderName() {
        return "HMS";
    }

    private static boolean callbackSuccessful;

    @Override
    public void subscribe(RegisteredHandler callback) {
        super.subscribe(callback);

        new Thread(() -> {
            try {
                getHMSTokenTask(context);
            } catch (ApiException e) {
                Log.e("CleverPush", "HMS ApiException getting Huawei token", e);
                this.tokenCallback(null);
            }
        }, "HMS_GET_TOKEN").start();
    }

    public void tokenCallback(String token) {
        if (registeredHandler == null) {
            return;
        }
        callbackSuccessful = true;

        if (token == null) {
            registeredHandler.complete(null);
        } else {
            this.syncSubscription(token, appId);
        }
    }

    private synchronized void getHMSTokenTask(@NonNull Context context) throws ApiException {
        Log.d("CleverPush", "Registering device with HMS App ID: " + appId);
        HmsInstanceId hmsInstanceId = HmsInstanceId.getInstance(context);

        String pushToken = hmsInstanceId.getToken(appId, HmsMessaging.DEFAULT_TOKEN_SCOPE);

        if (!TextUtils.isEmpty(pushToken)) {
            Log.d("CleverPush", "Device registered (HMS), push token = " + pushToken);
            this.tokenCallback(pushToken);
        } else {
            // Token is always null on Huawei EMUI <= 9. We need to wait for the event.
            waitForOnNewPushTokenEvent();
        }
    }

    private void waitForOnNewPushTokenEvent() {
        try {
            Thread.sleep(TOKEN_TIMEOUT_MS);
        } catch (InterruptedException e) {
        }

        if (!callbackSuccessful) {
            Log.e("CleverPush", "SubscriptionManagerHMS onNewToken timeout");
            this.tokenCallback(null);
        }
    }
}
