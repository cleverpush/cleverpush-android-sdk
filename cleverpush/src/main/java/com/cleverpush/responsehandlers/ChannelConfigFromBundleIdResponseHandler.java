package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import org.json.JSONObject;

public class ChannelConfigFromBundleIdResponseHandler {

  private InitializeListener initializeListener;
  private final CleverPush cleverPush;

  public ChannelConfigFromBundleIdResponseHandler(CleverPush cleverPush, InitializeListener initializeListener) {
    this.cleverPush = cleverPush;
    this.initializeListener = initializeListener;
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
          Logger.d(LOG_TAG,
              "Got Channel ID via Package Name: " + cleverPush.getChannelId(cleverPush.getContext()) + " (SDK "
                  + CleverPush.SDK_VERSION + ")");
        } catch (Throwable ex) {
          Logger.e(LOG_TAG, "Error in onSuccess of fetch Channel Config via Package Name.", ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        cleverPush.setInitialized(true);

        if (throwable != null) {
          Logger.e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
                  , throwable
          );
        } else {
          Logger.e("CleverPush", "Failed to fetch Channel Config via Package Name. Did you specify the package name in the CleverPush channel settings?." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }

        // trigger listeners
        if (cleverPush.getChannelConfig() == null) {
          SharedPreferences sharedPreferences = getSharedPreferences(getContext());
          String subscriptionId = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ID, null);
          cleverPush.fireSubscribedListener(subscriptionId);
          cleverPush.setSubscriptionId(subscriptionId);
          cleverPush.setChannelConfig(null);
          cleverPush.setInitialized(false);
          cleverPush.fireInitializeListener();
        }
      }
    };
  }

  public SharedPreferences getSharedPreferences(Context context) {
    return SharedPreferencesManager.getSharedPreferences(context);
  }

  public Context getContext() {
    return CleverPush.context;
  }
}
