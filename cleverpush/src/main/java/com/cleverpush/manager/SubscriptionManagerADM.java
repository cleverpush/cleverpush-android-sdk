package com.cleverpush.manager;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.util.Log;

import com.amazon.device.messaging.ADM;
import com.cleverpush.listener.SubscribedListener;

import org.json.JSONObject;

public class SubscriptionManagerADM extends SubscriptionManagerBase {

    public SubscriptionManagerADM(Context context) {
        super(context, SubscriptionManagerType.ADM);
    }

    @Override
    public void subscribe(JSONObject channelConfig, final SubscribedListener subscribedListener) {
        Context context = this.context;
        new Thread(() -> {
            final ADM adm = new ADM(context);
            String registrationId = adm.getRegistrationId();
            if (registrationId == null) {
                adm.startRegister();
            } else {
                Log.d(LOG_TAG, "ADM Already registered with ID:" + registrationId);
                this.syncSubscription(registrationId, subscribedListener);
            }
        }).start();
    }

    @Override
    public void checkChangedPushToken(JSONObject channelConfig, String changedToken) {

    }
}
