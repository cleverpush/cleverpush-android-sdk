package com.cleverpush.manager;

import android.content.Context;
import android.util.Log;

import com.amazon.device.messaging.ADM;

public class SubscriptionManagerADM extends SubscriptionManagerBase {

    public SubscriptionManagerADM(Context context) {
        super(context);
    }

    @Override
    public void subscribe(final RegisteredHandler callback) {
        super.subscribe(callback);

        Context context = this.context;
        new Thread(() -> {
            final ADM adm = new ADM(context);
            String registrationId = adm.getRegistrationId();
            if (registrationId == null) {
                adm.startRegister();
            } else {
                Log.d("CleverPush", "ADM Already registered with ID:" + registrationId);
                this.syncSubscription(registrationId);
            }
        }).start();
    }

    @Override
    public void tokenCallback(String token) {

    }

    @Override
    public String getProviderName() {
        return "ADM";
    }
}
