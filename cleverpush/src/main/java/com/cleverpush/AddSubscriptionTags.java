package com.cleverpush;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cleverpush.listener.AddTagCompletedListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AddSubscriptionTags implements AddTagCompletedListener {

    private String[] tagIds;
    private String subscriptionId;
    private String channelId;

    public AddSubscriptionTags(String subscriptionId, String channelId, String... tagIds) {
        this.subscriptionId = subscriptionId;
        this.channelId = channelId;
        this.tagIds = tagIds;
    }

    @Override
    public void tagAdded(int currentPositionOfTagToAdd) {
        if (currentPositionOfTagToAdd != tagIds.length - 1) {
            addSubscriptionTag(tagIds[++currentPositionOfTagToAdd], this, currentPositionOfTagToAdd);
        }
    }

    public void addMultipleSubscriptionTags() {
        addSubscriptionTag(tagIds[0], this, 0);
    }

    public void addSubscriptionTag() {
        addSubscriptionTag(tagIds[0], null, -1);
    }

    public void addSubscriptionTag(String tagId, AddTagCompletedListener addTagCompletedListener, int currentPositionOfTagToAdd) {
        if (subscriptionId != null) {
            Set<String> tags = this.getSubscriptionTags();
            if (tags.contains(tagId)) {
                if (addTagCompletedListener != null) {
                    addTagCompletedListener.tagAdded(currentPositionOfTagToAdd);
                }
                Log.d("CleverPush", "Subscription already has tag - skipping API call " + tagId);
                return;

            }

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("tagId", tagId);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
                Log.e("CleverPush", ex.getMessage(), ex);
            }

            tags.add(tagId);

            CleverPushHttpClient.post("/subscription/tag", jsonBody, addTagResponseHandler(tagId, addTagCompletedListener, currentPositionOfTagToAdd, tags));
        }
    }

    private CleverPushHttpClient.ResponseHandler addTagResponseHandler(String tagId, AddTagCompletedListener addTagCompletedListener, int currentPositionOfTagToAdd, Set<String> tags) {
        return new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {

                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS).apply();
                editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, tags);
                editor.commit();

                if (addTagCompletedListener != null) {
                    addTagCompletedListener.tagAdded(currentPositionOfTagToAdd);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e("CleverPush", "Error adding tag - HTTP " + statusCode);
            }
        };
    }

    public Set<String> getSubscriptionTags() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
    }
    
}
