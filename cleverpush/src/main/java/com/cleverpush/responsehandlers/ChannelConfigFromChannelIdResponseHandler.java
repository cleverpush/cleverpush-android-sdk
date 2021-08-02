package com.cleverpush.responsehandlers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;

import org.json.JSONObject;

public class ChannelConfigFromChannelIdResponseHandler {

    private static CleverPush instance;

    public ChannelConfigFromChannelIdResponseHandler(CleverPush instance) {
        this.instance = instance;
    }

    public static CleverPushHttpClient.ResponseHandler getResponseHandler(boolean autoRegister, String storedChannelId, String storedSubscriptionId) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                instance.setInitialized(true);

                try {
                    JSONObject responseJson = new JSONObject(response);
                    instance.setChannelConfig(responseJson);

                    instance.subscribeOrSync(autoRegister || instance.isChannelIdChanged(storedChannelId, storedSubscriptionId));

                    instance.initFeatures();

                } catch (Throwable ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                instance.setInitialized(true);

                Log.e("CleverPush", "Failed to fetch Channel Config", throwable);

                // trigger listeners
                if (instance.getChannelConfig() == null) {
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                    String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
                    instance.fireSubscribedListener(subscriptionId);
                    instance.setSubscriptionId(subscriptionId);
                    instance.setChannelConfig(null);
                }
            }
        };
    }

}
