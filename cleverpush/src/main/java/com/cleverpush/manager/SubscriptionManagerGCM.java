package com.cleverpush.manager;

import android.content.Context;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class SubscriptionManagerGCM extends SubscriptionManagerGoogle {

    private Context context;

    public SubscriptionManagerGCM(Context context) {
        super(context);
        this.context = context;
    }

    @Override
    public String getProviderName() {
        return "GCM";
    }

    @Override
    String getToken(String senderId) throws Throwable {
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this.context);
        return gcm.register(senderId);
    }
}
