package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;

import org.json.JSONObject;

public class ChannelConfigFromBundleIdResponseHandler {

    private final CleverPush cleverPush;

    public ChannelConfigFromBundleIdResponseHandler(CleverPush cleverPush) {
        this.cleverPush = cleverPush;
    }

    public CleverPushHttpClient.ResponseHandler getResponseHandler(boolean autoRegister) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                cleverPush.setInitialized(true);
                try {
                    JSONObject responseJson = new JSONObject(response);
                    cleverPush.setChannelConfig(responseJson);
                    cleverPush.setChannelId(responseJson.getString("channelId"));
                    cleverPush.subscribeOrSync(autoRegister);
                    cleverPush.initFeatures();
                    Log.d("CleverPush", "Got Channel ID via Package Name: " + cleverPush.getChannelId(cleverPush.getContext()) + " (SDK " + CleverPush.SDK_VERSION + ")");
                } catch (Throwable ex) {
                    Log.e("CleverPush", ex.getMessage(), ex);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                cleverPush.setInitialized(true);

                getLogger().e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?", throwable);

                // trigger listeners
                if (cleverPush.getChannelConfig() == null) {
                    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
                    String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
                    cleverPush.fireSubscribedListener(subscriptionId);
                    cleverPush.setSubscriptionId(subscriptionId);
                    cleverPush.setChannelConfig(null);
                }
            }
        };
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Context getContext() {
        return CleverPush.context;
    }

    public Logger getLogger() {
        return new Logger();
    }
}
