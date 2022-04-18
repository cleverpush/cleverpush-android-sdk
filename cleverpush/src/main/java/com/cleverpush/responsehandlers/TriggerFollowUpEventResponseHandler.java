package com.cleverpush.responsehandlers;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.util.Logger;

public class TriggerFollowUpEventResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler(String eventName) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                getLogger().d("CleverPush", "Follow-up event successfully tracked: " + eventName);
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                getLogger().e("CleverPush", "Error tracking follow-up event - HTTP " + statusCode);
            }
        };
    }

    public Logger getLogger() {
        return new Logger();
    }
}
