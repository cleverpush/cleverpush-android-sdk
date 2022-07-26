package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.AddTagCompletedListener;
import com.cleverpush.listener.AddTopicCompletedListener;
import com.cleverpush.listener.CompletionListener;
import com.cleverpush.listener.TopicsChangedListener;
import com.cleverpush.util.Logger;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SetSubscriptionTopicResponseHandler {

    public CleverPushHttpClient.ResponseHandler getResponseHandler(String topicId, AddTopicCompletedListener addTagCompletedListener, int currentPositionOfTagToAdd, Set<String> tags) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                updateSubscriptionTags(tags);
                if (addTagCompletedListener != null) {
                    addTagCompletedListener.topicAdded(currentPositionOfTagToAdd);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                getLogger().e("CleverPush", "Error adding tag - HTTP " + statusCode);
            }
        };
    }

    public void updateSubscriptionTags(Set<String> tags) {
        SharedPreferences sharedPreferences = getSharedPreferences(getContext());
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS);
        editor.apply();
        editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, tags);
        editor.commit();
    }

    public Set<String> getSubscriptionTags() {
        return getSharedPreferences(getContext()).getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
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
