package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;
import android.util.Log;

import com.cleverpush.listener.RemoveTopicCompletedListener;
import com.cleverpush.responsehandlers.RemoveSubscriptionTopicResponseHandler;
import com.cleverpush.util.ArrayUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class RemoveSubscriptionTopics implements RemoveTopicCompletedListener {

    private String[] topicIds;
    private final String subscriptionId;
    private final String channelId;
    private final SharedPreferences sharedPreferences;
    private boolean finished = false;
    Set<String> topics;

    public RemoveSubscriptionTopics(String subscriptionId, String channelId, SharedPreferences sharedPreferences, String... topicIds) {
        this.subscriptionId = subscriptionId;
        this.channelId = channelId;
        this.topicIds = topicIds;
        this.sharedPreferences = sharedPreferences;
    }


    public boolean isFinished() {
        return this.finished;
    }

    public void removeTopicIds(String... newTopicIds) {
        this.topicIds = ArrayUtils.concatenateArrays(this.topicIds, newTopicIds);
    }

    public void removeSubscriptionTopics() {
        if (topicIds == null || topicIds.length == 0) {
            return;
        }
        removeSubscriptionTopic(this, 0);
    }

    public void removeSubscriptionTag() {
        if (topicIds == null || topicIds.length == 0) {
            return;
        }
        removeSubscriptionTopic(null, 0);
    }

    public void removeSubscriptionTopic(RemoveTopicCompletedListener onRemoveTopicCompleted, int currentPositionOfTagToRemove) {
        if (subscriptionId != null) {
            JSONObject jsonBody = getJsonObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("topicId", topicIds[currentPositionOfTagToRemove]);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
            }

            topics = this.getSubscriptionTopics();
            topics.remove(topicIds[currentPositionOfTagToRemove]);

            CleverPushHttpClient.post("/subscription/topic/remove",
                    jsonBody,
                    new RemoveSubscriptionTopicResponseHandler().getResponseHandler(topicIds[currentPositionOfTagToRemove],
                            onRemoveTopicCompleted,
                            currentPositionOfTagToRemove,
                            topics));
        }
    }

    public Set<String> getSubscriptionTopics() {
        return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TOPICS, new HashSet<>());
    }

    public JSONObject getJsonObject() {
        return new JSONObject();
    }

    @Override
    public void topicRemoved(int currentPositionOfTopicToRemove) {
        if (currentPositionOfTopicToRemove != topicIds.length - 1) {
            removeSubscriptionTopic(this, currentPositionOfTopicToRemove + 1);
        } else {
            this.finished = true;
        }
    }
}
