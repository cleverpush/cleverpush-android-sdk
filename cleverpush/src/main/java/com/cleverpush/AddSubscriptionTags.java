package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;

import com.cleverpush.listener.CompletionFailureListener;
import com.cleverpush.util.Logger;

import com.cleverpush.listener.AddTagCompletedListener;
import com.cleverpush.responsehandlers.AddSubscriptionTagResponseHandler;
import com.cleverpush.util.ArrayUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class AddSubscriptionTags implements AddTagCompletedListener {

  private String[] tagIds;
  private final String subscriptionId;
  private final String channelId;
  private final SharedPreferences sharedPreferences;
  private CompletionFailureListener completionListener;
  private boolean finished = false;
  Set<String> tags;

  public AddSubscriptionTags(String subscriptionId, String channelId, SharedPreferences sharedPreferences,
                             CompletionFailureListener completionListener, String... tagIds) {
    this.subscriptionId = subscriptionId;
    this.channelId = channelId;
    this.tagIds = tagIds;
    this.sharedPreferences = sharedPreferences;
    this.completionListener = completionListener;
    finished = false;
  }

  @Override
  public void tagAdded(int currentPositionOfTagToAdd) {
    if (currentPositionOfTagToAdd != tagIds.length - 1) {
      addSubscriptionTag(this, currentPositionOfTagToAdd + 1);
    } else {
      if (completionListener != null) {
        completionListener.onComplete();
      }
      this.finished = true;
    }
  }

  @Override
  public void onFailure(Exception exception) {
    if (completionListener != null) {
      completionListener.onFailure(exception);
    }
    this.finished = true;
  }

  public boolean isFinished() {
    return this.finished;
  }

  public void addTagIds(String... newTagIds) {
    this.tagIds = ArrayUtils.concatenateArrays(this.tagIds, newTagIds);
  }

  public void addSubscriptionTags() {
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
      tags = this.getSubscriptionTags();
      if (tags.contains(tagIds[currentPositionOfTagToAdd])) {
        if (addTagCompletedListener != null) {
          addTagCompletedListener.tagAdded(currentPositionOfTagToAdd);
        }
        Logger.d(LOG_TAG, "Subscription already has tag - skipping API call " + tagIds[currentPositionOfTagToAdd]);
        return;
      }

      JSONObject jsonBody = getJsonObject();
      try {
        jsonBody.put("channelId", this.channelId);
        jsonBody.put("tagId", tagIds[currentPositionOfTagToAdd]);
        jsonBody.put("subscriptionId", subscriptionId);
      } catch (JSONException ex) {
        Logger.e(LOG_TAG, ex.getMessage(), ex);
      }

      CleverPushHttpClient.postWithRetry("/subscription/tag", jsonBody,
          new AddSubscriptionTagResponseHandler().getResponseHandler(tagIds[currentPositionOfTagToAdd],
              addTagCompletedListener, currentPositionOfTagToAdd, tags));
    } else {
      Logger.d(LOG_TAG, "addSubscriptionTag: There is no subscription for CleverPush SDK.");
    }
  }

  public Set<String> getSubscriptionTags() {
    return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
  }

  public JSONObject getJsonObject() {
    return new JSONObject();
  }
}
