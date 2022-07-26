package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;
import android.util.Log;

import com.cleverpush.listener.AddTopicCompletedListener;
import com.cleverpush.responsehandlers.SetSubscriptionTopicResponseHandler;
import com.cleverpush.util.ArrayUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AddSubscriptionTopic implements AddTopicCompletedListener {
    private String[] topicIds;
    private final String subscriptionId;
    private final String channelId;
    private final SharedPreferences sharedPreferences;
    private boolean finished = false;
    Set<String> topics;

    public AddSubscriptionTopic(String subscriptionId, String channelId, SharedPreferences sharedPreferences, String... topicIds) {
        this.subscriptionId = subscriptionId;
        this.channelId = channelId;
        this.topicIds = topicIds;
        this.sharedPreferences = sharedPreferences;
    }

    public boolean isFinished() {
        return this.finished;
    }

    public void addTopicIds(String... newTopicIds) {
        this.topicIds = ArrayUtils.concatenateArrays(this.topicIds, newTopicIds);
    }

    public void addSubscriptionTopics() {
        if (topicIds == null || topicIds.length == 0) {
            return;
        }
        addSubscriptionTopic(this, 0);
    }

    public void addSubscriptionTopic() {
        if (topicIds == null || topicIds.length == 0) {
            return;
        }
        addSubscriptionTopic(null, 0);
    }

    public void addSubscriptionTopic(AddTopicCompletedListener addTopicCompletedListener, int currentPositionOfTopicToAdd) {
        if (subscriptionId != null) {
            topics = this.getSubscriptionTopics();
            if (topics.contains(topicIds[currentPositionOfTopicToAdd])) {
                if (addTopicCompletedListener != null) {
                    addTopicCompletedListener.topicAdded(currentPositionOfTopicToAdd);
                }
                Log.d(LOG_TAG, "Subscription already has topic - skipping API call " + topicIds[currentPositionOfTopicToAdd]);
                return;
            }

            JSONObject jsonBody = getJsonObject();
            try {
                jsonBody.put("channelId", this.channelId);
                jsonBody.put("topicId", topicIds[currentPositionOfTopicToAdd]);
                jsonBody.put("subscriptionId", subscriptionId);
            } catch (JSONException ex) {
                Log.e(LOG_TAG, ex.getMessage(), ex);
            }

            topics.add(topicIds[currentPositionOfTopicToAdd]);

            CleverPushHttpClient.post("/subscription/topic/add",
                    jsonBody,
                    new SetSubscriptionTopicResponseHandler().getResponseHandler(topicIds[currentPositionOfTopicToAdd],
                            addTopicCompletedListener,
                            currentPositionOfTopicToAdd,
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
    public void topicAdded(int currentPositionOfTagToAdd) {

    }
}
