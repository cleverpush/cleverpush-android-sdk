package com.cleverpush.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.messaging.FirebaseMessaging;

public class SubscriptionManagerFCM extends SubscriptionManagerGoogle {

    private FirebaseApp firebaseApp;

    public SubscriptionManagerFCM(Context context) {
        super(context);
    }

    public static void disableFirebaseInstanceIdService(Context context) {
        String senderId = null;
        Resources resources = context.getResources();
        int bodyResId = resources.getIdentifier("gcm_defaultSenderId", "string", context.getPackageName());
        if (bodyResId != 0) {
            senderId = resources.getString(bodyResId);
        }

        int componentState =
                senderId == null ?
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED :
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        PackageManager pm = context.getPackageManager();
        try {
            ComponentName componentName = new ComponentName(context, FirebaseInstanceIdService.class);
            pm.setComponentEnabledSetting(componentName, componentState, PackageManager.DONT_KILL_APP);
        } catch (NoClassDefFoundError ignored) {

        }
    }

    @Override
    public String getProviderName() {
        return "FCM";
    }

    @Override
    String getToken(String senderId) throws Throwable {
        initFirebaseApp(senderId);
        FirebaseInstanceId instanceId = FirebaseInstanceId.getInstance(firebaseApp);
        return instanceId.getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE);
    }

    private void initFirebaseApp(String senderId) {
        if (firebaseApp != null) {
            return;
        }

        FirebaseOptions firebaseOptions =
                new FirebaseOptions.Builder()
                        .setGcmSenderId(senderId)
                        .setApplicationId("OMIT_ID")
                        .setApiKey("OMIT_KEY")
                        .build();
        firebaseApp = FirebaseApp.initializeApp(this.context, firebaseOptions, "CLEVERPUSH_SDK_APP_NAME");
    }
}
