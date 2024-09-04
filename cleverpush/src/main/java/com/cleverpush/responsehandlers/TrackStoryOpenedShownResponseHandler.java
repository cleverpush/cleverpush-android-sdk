package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.util.Logger;

public class TrackStoryOpenedShownResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler(boolean isOpened) {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        if (isOpened) {
          Logger.d(LOG_TAG, "Story Opened");
        } else {
          Logger.d(LOG_TAG, "Story Shown");
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        String message;
        if (isOpened) {
          message = "Failed to track story open.";
        } else {
          message = "Failed to track story shown.";
        }
        if (throwable != null) {
          Logger.e(LOG_TAG, message +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
              , throwable
          );
        } else {
          Logger.e(LOG_TAG, message +
              "\nStatus code: " + statusCode +
              "\nResponse: " + response
          );
        }
      }
    };
  }
}
