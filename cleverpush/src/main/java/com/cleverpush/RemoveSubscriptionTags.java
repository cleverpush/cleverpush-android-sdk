package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;

import com.cleverpush.listener.CompletionFailureListener;
import com.cleverpush.util.Logger;

import com.cleverpush.listener.RemoveTagCompletedListener;
import com.cleverpush.responsehandlers.RemoveSubscriptionTagResponseHandler;
import com.cleverpush.util.ArrayUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

public class RemoveSubscriptionTags implements RemoveTagCompletedListener {

  private String[] tagIds;
  private final String subscriptionId;
  private final String channelId;
  private final SharedPreferences sharedPreferences;
  private boolean finished = false;
  private CompletionFailureListener completionListener;
  Set<String> tags;

  public RemoveSubscriptionTags(String subscriptionId, String channelId, SharedPreferences sharedPreferences,
                                CompletionFailureListener completionListener, String... tagIds) {
    this.subscriptionId = subscriptionId;
    this.channelId = channelId;
    this.tagIds = tagIds;
    this.sharedPreferences = sharedPreferences;
    this.completionListener = completionListener;
    finished = false;
  }

  @Override
  public void tagRemoved(int currentPositionOfTagToRemove) {
    if (currentPositionOfTagToRemove != tagIds.length - 1) {
      removeSubscriptionTag(this, currentPositionOfTagToRemove + 1);
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
        Logger.e(LOG_TAG, ex.getMessage(), ex);
      }

      tags = this.getSubscriptionTags();

      CleverPushHttpClient.postWithRetry("/subscription/untag", jsonBody,
          new RemoveSubscriptionTagResponseHandler().getResponseHandler(tagIds[currentPositionOfTagToRemove],
              onRemoveTagCompleted, currentPositionOfTagToRemove, tags));
    } else {
      Logger.d(LOG_TAG, "removeSubscriptionTag: There is no subscription for CleverPush SDK.");
    }
  }

  public Set<String> getSubscriptionTags() {
    return sharedPreferences.getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
  }

  public JSONObject getJsonObject() {
    return new JSONObject();
  }
}
