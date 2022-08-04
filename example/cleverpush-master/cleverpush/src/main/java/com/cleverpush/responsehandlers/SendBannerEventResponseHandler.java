package com.cleverpush.responsehandlers;

import android.util.Log;

import com.cleverpush.CleverPushHttpClient;

public class SendBannerEventResponseHandler {

    private static final String TAG = "CleverPush/AppBanner";

    public CleverPushHttpClient.ResponseHandler getResponseHandler() {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d(TAG, "App Banner Event success.");
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
