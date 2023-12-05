package com.cleverpush.responsehandlers;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.StopCampaignListener;
import com.cleverpush.util.Logger;

public class StopCampaignResponseHandler {

  private final CleverPush cleverPush;
  private final StopCampaignListener listener;

  public StopCampaignResponseHandler(CleverPush cleverPush, StopCampaignListener listener) {
    this.cleverPush = cleverPush;
    this.listener = listener;
  }

  public CleverPushHttpClient.ResponseHandler getResponseHandler() {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d("CleverPush", "StopCampaign success");

        if (listener != null) {
          listener.onSuccess();
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e("CleverPush", "Failed while stopCampaign request." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
          );
        } else {
          Logger.e("CleverPush", "Failed while stopCampaign request." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
        if (listener != null) {
          listener.onFailure(throwable);
        }
      }
    };
  }
}
