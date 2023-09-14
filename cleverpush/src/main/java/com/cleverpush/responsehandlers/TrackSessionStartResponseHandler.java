package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import com.cleverpush.util.Logger;

import com.cleverpush.CleverPushHttpClient;

public class TrackSessionStartResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler() {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d(LOG_TAG, "Session started");
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e(LOG_TAG, "Failed to track session start." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e(LOG_TAG, "Failed to track session start." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };
  }

}
