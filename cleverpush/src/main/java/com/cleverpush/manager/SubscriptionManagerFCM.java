package com.cleverpush.manager;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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

        int componentState = senderId == null ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

        PackageManager pm = context.getPackageManager();
        try {
            ComponentName componentName = new ComponentName(context, Class.forName("com.google.firebase.iid.FirebaseInstanceIdService"));
            pm.setComponentEnabledSetting(componentName, componentState, PackageManager.DONT_KILL_APP);
        } catch (Exception ignored) {

        }
    }

    @Override
    public void tokenCallback(String token) {

    }

    @Override
    public String getProviderName() {
        return "FCM";
    }

    @Override
    String getToken(String senderId) throws Throwable {
        initFirebaseApp(senderId);

        try {
            return getTokenWithClassFirebaseMessaging();
        } catch (Exception e) {
            Log.d("CleverPush", "FirebaseMessaging.getToken not found, attempting to use FirebaseInstanceId.getToken");
        }
        return getTokenWithClassFirebaseInstanceId(senderId);
    }

    private void initFirebaseApp(String senderId) {
        if (firebaseApp != null) {
            return;
        }

        firebaseApp = FirebaseApp.getInstance();
    }

    /**
     * Will use to get Firebase token if firebase-message older than 21.0.0
     */
    @Deprecated
    @WorkerThread
    private String getTokenWithClassFirebaseInstanceId(String senderId) throws IOException {
        Exception exception;
        try {
            Class<?> FirebaseInstanceIdClass = Class.forName("com.google.firebase.iid.FirebaseInstanceId");
            Method getInstanceMethod = FirebaseInstanceIdClass.getMethod("getInstance", FirebaseApp.class);
            Object instanceId = getInstanceMethod.invoke(null, firebaseApp);
            Method getTokenMethod = instanceId.getClass().getMethod("getToken", String.class, String.class);
            Object token = getTokenMethod.invoke(instanceId, senderId, "FCM");
            return (String) token;
        } catch (ClassNotFoundException e) {
            exception = e;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception = e;
        } catch (InvocationTargetException e) {
            exception = e;
        }

        throw new Error("Reflection error on FirebaseInstanceId.getInstance(firebaseApp).getToken(senderId, FirebaseMessaging.INSTANCE_ID_SCOPE)", exception);
    }

    /**
     * Will use to get Firebase token if firebase-message newer than 20.0.0
     */
    @WorkerThread
    private String getTokenWithClassFirebaseMessaging() throws Exception {
        Exception exception;
        try {
            Class<?> FirebaseInstanceIdClass = Class.forName("com.google.firebase.messaging.FirebaseMessaging");
            Method getInstanceMethod = FirebaseInstanceIdClass.getMethod("getInstance");
            Object instanceId = getInstanceMethod.invoke(null, null);
            Method getTokenMethod = FirebaseInstanceIdClass.getMethod("getToken");
            Object token = getTokenMethod.invoke(instanceId, null);
            return String.valueOf(((Task<String>) token).getResult());
        } catch (ClassNotFoundException e) {
            exception = e ;
        } catch (NoSuchMethodException e) {
            exception = e;
        } catch (IllegalAccessException e) {
            exception =  new IllegalAccessException();
        } catch (InvocationTargetException e) {
            exception = e;
        }
        throw exception;
    }
}
