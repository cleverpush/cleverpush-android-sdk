package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.CompletionFailureListener;
import com.cleverpush.util.Logger;

public class MarkSubscriptionAsTestResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler(CompletionFailureListener completionListener) {
    return new CleverPushHttpClient.ResponseHandler() {

      @Override
      public void onSuccess(String response) {
        Logger.d(LOG_TAG, "markSubscriptionAsTest: Successfully marked subscription as test");
        if (completionListener != null) {
          completionListener.onComplete();
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {

        String errorMessage = "markSubscriptionAsTest: Failed to mark subscription as test." +
                "\nStatus code: " + statusCode +
                "\nResponse: " + response;

        if (throwable != null) {
          errorMessage += "\nError: " + throwable.getMessage();
          Logger.e(LOG_TAG, errorMessage, throwable);

          if (completionListener != null) {
            completionListener.onFailure(new Exception(errorMessage, throwable));
          }
        } else {
          Logger.e(LOG_TAG, errorMessage);

          if (completionListener != null) {
            completionListener.onFailure(new Exception(errorMessage));
          }
        }
      }
    };
  }
}
