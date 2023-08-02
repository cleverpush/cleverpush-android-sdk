package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;

import org.json.JSONObject;

public class ChannelConfigFromChannelIdResponseHandler {

  private final CleverPush cleverPush;

  public ChannelConfigFromChannelIdResponseHandler(CleverPush instance) {
    this.cleverPush = instance;
  }

  public CleverPushHttpClient.ResponseHandler getResponseHandler(boolean autoRegister, String storedChannelId,
                                                                 String storedSubscriptionId) {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        cleverPush.setInitialized(true);

        try {
          JSONObject responseJson = new JSONObject(response);
          cleverPush.setChannelConfig(responseJson);

          cleverPush.subscribeOrSync(
              autoRegister || cleverPush.isChannelIdChanged(storedChannelId, storedSubscriptionId));

          cleverPush.initFeatures();

        } catch (Throwable ex) {
          Logger.e(LOG_TAG, ex.getMessage(), ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        cleverPush.setInitialized(true);

        Logger.e("CleverPush", "Failed to fetch Channel Config", throwable);

        // trigger listeners
        if (cleverPush.getChannelConfig() == null) {
          SharedPreferences sharedPreferences = getSharedPreferences(getContext());
          String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
          cleverPush.fireSubscribedListener(subscriptionId);
          cleverPush.setSubscriptionId(subscriptionId);
          cleverPush.setChannelConfig(null);
          cleverPush.setInitialized(false);
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
}
