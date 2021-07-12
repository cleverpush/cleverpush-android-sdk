package com.cleverpush;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.listener.RemoveTagCompletedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class RemoveSubscriptionTags implements RemoveTagCompletedListener {

    private String[] tagIds;
    private String subscriptionId;
    private String channelId;

    public RemoveSubscriptionTags(String subscriptionId, String channelId, String... tagIds) {
        this.subscriptionId = subscriptionId;
        this.channelId = channelId;
        this.tagIds = tagIds;
    }

    @Override
    public void tagRemoved(int currentPositionOfTagToRemove) {
        if (currentPositionOfTagToRemove != tagIds.length - 1) {
            removeSubscriptionTag(this, currentPositionOfTagToRemove++);
        }
    }

    public void removeMultipleSubscriptionSubscriptionTags() {
        if (tagIds == null || tagIds.length == 0) {
            return;
        }
        removeSubscriptionTag(this, 0);
    }

    public void removeSubscriptionTag() {
        if (tagIds == null || tagIds.length == 0) {
            return;
        }
        removeSubscriptionTag(null, 0);
    }

    public void removeSubscriptionTag(RemoveTagCompletedListener onRemoveTagCompleted, int currentPositionOfTagToRemove) {
        if (subscriptionId != null) {
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("tagId", tagIds[currentPositionOfTagToRemove]);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
                Log.e("CleverPush", ex.getMessage(), ex);
            }

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
            Set<String> tags = this.getSubscriptionTags();
            tags.remove(tagIds[currentPositionOfTagToRemove]);

            CleverPushHttpClient.post("/subscription/untag", jsonBody, removeSubscriptionTagResponseHandler(tagIds[currentPositionOfTagToRemove], onRemoveTagCompleted, currentPositionOfTagToRemove, tags));
        }
    }

    public CleverPushHttpClient.ResponseHandler removeSubscriptionTagResponseHandler(String tagId, RemoveTagCompletedListener onRemoveTagCompleted, int currentPositionOfTagToRemove, Set<String> tags) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        CleverPushHttpClient.ResponseHandler responseHandler = new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS).apply();
                editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, tags);
                editor.commit();
                if (onRemoveTagCompleted != null) {
                    onRemoveTagCompleted.tagRemoved(currentPositionOfTagToRemove);
                }
                Log.e("removedTag", tagId);


            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e("CleverPush", "Error removing tag - HTTP " + statusCode);
            }
        };
        return responseHandler;
    }

    public Set<String> getSubscriptionTags() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
    }

}
