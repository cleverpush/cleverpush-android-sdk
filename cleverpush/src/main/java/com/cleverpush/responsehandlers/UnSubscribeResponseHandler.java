package com.cleverpush.responsehandlers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;

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
                    getLogger().d("CleverPush", "unsubscribe success");
                    cleverPush.clearSubscriptionData();

                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(CleverPushPreferences.UNSUBSCRIBED, true);
                    editor.commit();
                } catch (Throwable t) {
                    getLogger().e("CleverPush", "Error", t);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable t) {
                getLogger().e("CleverPush", "Failed while unsubscribe request - " + statusCode + " - " + response, t);
            }
        };
    }

    public Logger getLogger() {
        return new Logger();
    }
}
