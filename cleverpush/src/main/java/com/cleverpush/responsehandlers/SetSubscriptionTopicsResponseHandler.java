package com.cleverpush.responsehandlers;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.listener.CompletionListener;
import com.cleverpush.listener.TopicsChangedListener;
import com.cleverpush.util.Logger;

import java.util.Arrays;
import java.util.HashSet;

public class SetSubscriptionTopicsResponseHandler {

    private final CleverPush cleverPush;

    public SetSubscriptionTopicsResponseHandler(CleverPush cleverPush) {
        this.cleverPush = cleverPush;
    }

    public CleverPushHttpClient.ResponseHandler getResponseHandler(String[] topicIds, CompletionListener completionListener) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                TopicsChangedListener topicsChangedListener = cleverPush.getTopicsChangedListener();
                if (topicsChangedListener != null) {
                    topicsChangedListener.changed(new HashSet<>(Arrays.asList(topicIds)));
                }
                if (completionListener != null) {
                    completionListener.onComplete();
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Logger.e("CleverPush", "Error setting topics - HTTP " + statusCode + ": " + response);
            }
        };
    }
}
