package com.cleverpush.util;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

// We use this to catch exceptions so that the App doesn't crash in case an exception is thrown inside of the Runnable
public class CustomExceptionHandler extends Handler {
    private static final String TAG = "CleverPush/Utils";

    public CustomExceptionHandler(Looper looper) {
        super(looper);
    }

    @Override
    public void dispatchMessage(Message msg) {
        try {
            super.dispatchMessage(msg);
        } catch (Exception e) {
            Logger.d(TAG, "Exception inside of Handler runnable " + e.getLocalizedMessage());
        }
    }
}
