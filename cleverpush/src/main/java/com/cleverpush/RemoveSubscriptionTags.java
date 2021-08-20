package com.cleverpush;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.listener.RemoveTagCompletedListener;
import com.cleverpush.responsehandlers.RemoveSubscriptionTagResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class RemoveSubscriptionTags implements RemoveTagCompletedListener {

    private String[] tagIds;
    private String subscriptionId;
    private String channelId;
    Set<String> tags;
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

    public void removeSubscriptionTags() {
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
            JSONObject jsonBody = getJsonObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("tagId", tagIds[currentPositionOfTagToRemove]);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
                Log.e("CleverPush", ex.getMessage(), ex);
            }

            tags = this.getSubscriptionTags();
            tags.remove(tagIds[currentPositionOfTagToRemove]);

            CleverPushHttpClient.post("/subscription/untag", jsonBody, new RemoveSubscriptionTagResponseHandler().getResponseHandler(tagIds[currentPositionOfTagToRemove], onRemoveTagCompleted, currentPositionOfTagToRemove, tags));
        }
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

    public JSONObject getJsonObject() {
        return new JSONObject();
    }
}
