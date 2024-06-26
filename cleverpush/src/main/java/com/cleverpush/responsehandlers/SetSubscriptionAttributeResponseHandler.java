package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import org.json.JSONObject;

import java.util.Map;

public class SetSubscriptionAttributeResponseHandler {

  private Runnable successCallback;

  public SetSubscriptionAttributeResponseHandler(Runnable suppliedSuccessCallback) {
    successCallback = suppliedSuccessCallback;
  }

  public SetSubscriptionAttributeResponseHandler() {
  }

  public CleverPushHttpClient.ResponseHandler getResponseHandler(Map<String, Object> subscriptionAttributes) {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        try {
          SharedPreferences sharedPreferences = getSharedPreferences(getContext());
          if (sharedPreferences != null) {
            JSONObject jsonObject = new JSONObject(subscriptionAttributes);
            String jsonString = jsonObject.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES);
            editor.apply();
            editor.putString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, jsonString);
            editor.apply();
          }

          if (successCallback != null) {
            successCallback.run();
          }
        } catch (Exception ex) {
          Logger.e(LOG_TAG, "Error in onSuccess of setting subscription attribute", ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e("CleverPush", "Error setting attribute." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
                  , throwable
          );
        } else {
          Logger.e("CleverPush", "Error setting attribute." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
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
