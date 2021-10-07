package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.AddTagCompletedListener;
import com.cleverpush.util.Logger;

import java.util.HashSet;
import java.util.Set;

public class TrackEventResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler(String eventName) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                getLogger().d("CleverPush", "Event successfully tracked: " + eventName);
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                getLogger().e("CleverPush", "Error tracking event - HTTP " + statusCode);
            }
        };
    }

    public Logger getLogger() {
        return new Logger();
    }
}
