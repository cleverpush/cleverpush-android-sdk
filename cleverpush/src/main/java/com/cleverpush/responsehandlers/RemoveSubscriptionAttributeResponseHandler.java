package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.RemoveAttributeCompletedListener;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import org.json.JSONObject;

import java.util.Map;

public class RemoveSubscriptionAttributeResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler(Map<String, Object> subscriptionAttributes,
                                                                 RemoveAttributeCompletedListener removeAttributeCompletedListener,
                                                                 int currentPositionOfAttributeToRemove) {
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

          if (removeAttributeCompletedListener != null) {
            removeAttributeCompletedListener.attributeRemoved(currentPositionOfAttributeToRemove);
          }
        } catch (Exception ex) {
          Logger.e(LOG_TAG, "Error in onSuccess of removing subscription attribute", ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e("CleverPush", "Error removing subscription attribute." +
                          "\nStatus code: " + statusCode +
                          "\nResponse: " + response +
                          "\nError: " + throwable.getMessage()
                  , throwable
          );
          if (removeAttributeCompletedListener != null) {
            removeAttributeCompletedListener.onFailure(new Exception("Error removing subscription attribute - HTTP " + statusCode + ": " + response + "\nError: " + throwable.getMessage()));
          }
        } else {
          Logger.e("CleverPush", "Error removing subscription attribute." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
          if (removeAttributeCompletedListener != null) {
            removeAttributeCompletedListener.onFailure(new Exception("Error removing subscription attribute." +
                    "\nStatus code: " + statusCode +
                    "\nResponse: " + response));
          }
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
