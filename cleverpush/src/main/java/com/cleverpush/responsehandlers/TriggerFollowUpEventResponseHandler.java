package com.cleverpush.responsehandlers;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.util.Logger;

public class TriggerFollowUpEventResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler(String eventName) {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d("CleverPush", "Follow-up event successfully tracked: " + eventName);
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e("CleverPush", "Error tracking follow-up event." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e("CleverPush", "Error tracking follow-up event." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };
  }
}
