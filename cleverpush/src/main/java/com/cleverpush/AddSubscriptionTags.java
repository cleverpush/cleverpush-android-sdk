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
            addSubscriptionTag(this, currentPositionOfTagToAdd++);
        }
    }

    public void addMultipleSubscriptionTags() {
        if (tagIds == null || tagIds.length == 0) {
            return;
        }
        addSubscriptionTag(this, 0);
    }

    public void addSubscriptionTag() {
        if (tagIds == null || tagIds.length == 0) {
            return;
        }
        addSubscriptionTag(null, 0);
    }

    public void addSubscriptionTag(AddTagCompletedListener addTagCompletedListener, int currentPositionOfTagToAdd) {
        if (subscriptionId != null) {
            Set<String> tags = this.getSubscriptionTags();
            if (tags.contains(tagIds[currentPositionOfTagToAdd])) {
                if (addTagCompletedListener != null) {
                    addTagCompletedListener.tagAdded(currentPositionOfTagToAdd);
                }
                Log.d("CleverPush", "Subscription already has tag - skipping API call " + tagIds[currentPositionOfTagToAdd]);
                return;

            }

            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("tagId", tagIds[currentPositionOfTagToAdd]);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
                Log.e("CleverPush", ex.getMessage(), ex);
            }

            tags.add(tagIds[currentPositionOfTagToAdd]);

            CleverPushHttpClient.post("/subscription/tag", jsonBody, addTagResponseHandler(tagIds[currentPositionOfTagToAdd], addTagCompletedListener, currentPositionOfTagToAdd, tags));
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
