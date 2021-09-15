package com.cleverpush.responsehandlers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;

import org.json.JSONObject;

public class UnSubscribeResponseHandler {

    private static CleverPush cleverPush;

    public UnSubscribeResponseHandler(CleverPush cleverPush) {
        this.cleverPush = cleverPush;
    }

    public CleverPushHttpClient.ResponseHandler getResponseHandler() {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                try {
                    Log.d("CleverPush", "unsubscribe success");
                    cleverPush.clearSubscriptionData();
                } catch (Throwable t) {
                    Log.e("CleverPush", "Error", t);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable t) {
                Log.e("CleverPush", "Failed while unsubscribe request - " + statusCode + " - " + response, t);
            }
        };
    }

}
