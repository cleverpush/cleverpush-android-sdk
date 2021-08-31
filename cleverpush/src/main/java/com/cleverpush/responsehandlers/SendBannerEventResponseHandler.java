package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.RemoveTagCompletedListener;

import java.util.HashSet;
import java.util.Set;

public class SendBannerEventResponseHandler {

    private static final String TAG = "CleverPush/AppBanner";

    public CleverPushHttpClient.ResponseHandler getResponseHandler() {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {

                Log.e(TAG, "App Banner Event success.");
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e(TAG, "App Banner Event failed." +
                        "\nStatus code: " + statusCode +
                        "\nResponse: " + response
                );
            }
        };

    }
}
