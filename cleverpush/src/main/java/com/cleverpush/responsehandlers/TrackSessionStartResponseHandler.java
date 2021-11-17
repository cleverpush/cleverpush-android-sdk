package com.cleverpush.responsehandlers;

import android.util.Log;

import com.cleverpush.CleverPushHttpClient;

public class TrackSessionStartResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler() {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d("CleverPush", "Session started");
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e("CleverPush", "Error setting topics - HTTP " + statusCode + ": " + response);
            }
        };
    }

}
