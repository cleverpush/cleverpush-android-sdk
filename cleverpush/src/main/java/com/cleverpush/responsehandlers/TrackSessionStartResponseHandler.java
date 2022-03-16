package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import android.util.Log;

import com.cleverpush.CleverPushHttpClient;

public class TrackSessionStartResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler() {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                Log.d(LOG_TAG, "Session started");
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e(LOG_TAG, "Error setting topics - HTTP " + statusCode + ": " + response);
            }
        };
    }

}
