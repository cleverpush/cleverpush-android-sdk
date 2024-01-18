package com.cleverpush.responsehandlers;

import com.cleverpush.util.Logger;

import com.cleverpush.CleverPushHttpClient;

public class SendBannerEventResponseHandler {

  private static final String TAG = "CleverPush/AppBanner";

  public CleverPushHttpClient.ResponseHandler getResponseHandler() {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d(TAG, "App Banner Event success.");
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e(TAG, "App Banner Event failed." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
                  , throwable
          );
        } else {
          Logger.e(TAG, "App Banner Event failed." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };

  }
}
