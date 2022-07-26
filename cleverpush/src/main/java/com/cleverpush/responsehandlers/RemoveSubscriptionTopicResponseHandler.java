package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.RemoveTopicCompletedListener;
import com.cleverpush.util.Logger;

import java.util.HashSet;
import java.util.Set;

public class RemoveSubscriptionTopicResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler(String topicId, RemoveTopicCompletedListener removeTopicCompletedListener, int currentPositionOfTopicToRemove, Set<String> topics) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                updateSubscriptionTopics(topics);
                if (removeTopicCompletedListener != null) {
                    removeTopicCompletedListener.topicRemoved(currentPositionOfTopicToRemove);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                getLogger().e("CleverPush", "Error removing topic - HTTP " + statusCode);
            }
        };

    }

    public void updateSubscriptionTopics(Set<String> topics) {
        SharedPreferences sharedPreferences = getSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(CleverPushPreferences.SUBSCRIPTION_TOPICS);
        editor.apply();
        editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, topics);
        editor.commit();
    }

    public Set<String> getSubscriptionTopics() {
        return getSharedPreferences(getContext()).getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>());
    }

    public SharedPreferences getSharedPreferences(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public Context getContext() {
        return CleverPush.context;
    }

    public Logger getLogger() {
        return new Logger();
    }
}
