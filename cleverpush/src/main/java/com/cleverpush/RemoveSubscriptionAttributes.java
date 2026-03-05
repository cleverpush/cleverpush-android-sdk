package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.SharedPreferences;

import com.cleverpush.listener.CompletionFailureListener;
import com.cleverpush.listener.RemoveAttributeCompletedListener;
import com.cleverpush.responsehandlers.RemoveSubscriptionAttributeResponseHandler;
import com.cleverpush.util.ArrayUtils;
import com.cleverpush.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RemoveSubscriptionAttributes implements RemoveAttributeCompletedListener {

  private String[] attributeIds;
  private final String subscriptionId;
  private final String channelId;
  private final SharedPreferences sharedPreferences;
  private boolean finished = false;
  private CompletionFailureListener completionListener;

  public RemoveSubscriptionAttributes(String subscriptionId, String channelId, SharedPreferences sharedPreferences,
                                      CompletionFailureListener completionListener, String... attributeIds) {
    this.subscriptionId = subscriptionId;
    this.channelId = channelId;
    this.attributeIds = attributeIds;
    this.sharedPreferences = sharedPreferences;
    this.completionListener = completionListener;
    finished = false;
  }

  @Override
  public void attributeRemoved(int currentPositionOfAttributeToRemove) {
    if (currentPositionOfAttributeToRemove != attributeIds.length - 1) {
      removeSubscriptionAttribute(this, currentPositionOfAttributeToRemove + 1);
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

  public void addAttributeIds(String... newAttributeIds) {
    this.attributeIds = ArrayUtils.concatenateArrays(this.attributeIds, newAttributeIds);
  }

  public void removeSubscriptionAttributes() {
    if (attributeIds == null || attributeIds.length == 0) {
      return;
    }
    removeSubscriptionAttribute(this, 0);
  }

  public void removeSubscriptionAttribute(RemoveAttributeCompletedListener onRemoveAttributeCompleted, int currentPositionOfAttributeToRemove) {
    if (attributeIds == null || attributeIds.length == 0
        || currentPositionOfAttributeToRemove < 0 || currentPositionOfAttributeToRemove >= attributeIds.length) {
      if (onRemoveAttributeCompleted != null) {
        onRemoveAttributeCompleted.onFailure(new IllegalArgumentException(
            "Invalid attribute position: " + currentPositionOfAttributeToRemove
                + (attributeIds == null ? " (attributeIds is null)" : " (attributeIds.length=" + attributeIds.length + ")")));
      }
      return;
    }
    if (subscriptionId != null && !subscriptionId.isEmpty()) {
      JSONObject jsonBody = getJsonObject();
      try {
        jsonBody.put("channelId", this.channelId);
        jsonBody.put("attributeId", attributeIds[currentPositionOfAttributeToRemove]);
        jsonBody.put("subscriptionId", subscriptionId);
      } catch (JSONException ex) {
        Logger.e(LOG_TAG, "Error in removeSubscriptionAttribute(/subscription/attribute/clear/) request parameter", ex);
      }

      Map<String, Object> subscriptionAttributes = getSubscriptionAttributes();
      subscriptionAttributes.remove(attributeIds[currentPositionOfAttributeToRemove]);

      CleverPushHttpClient.postWithRetry("/subscription/attribute/clear", jsonBody,
          new RemoveSubscriptionAttributeResponseHandler().getResponseHandler(subscriptionAttributes,
              onRemoveAttributeCompleted, currentPositionOfAttributeToRemove));
    } else {
      Logger.d(LOG_TAG, "removeSubscriptionAttribute: There is no subscription for CleverPush SDK.");
    }
  }

  public Map<String, Object> getSubscriptionAttributes() {
    Map<String, Object> outputMap = new HashMap<>();
    try {
      if (sharedPreferences != null) {
        String jsonString = sharedPreferences.getString(CleverPushPreferences.SUBSCRIPTION_ATTRIBUTES, (new JSONObject()).toString());
        JSONObject jsonObject = new JSONObject(jsonString);
        Iterator<String> keysItr = jsonObject.keys();
        while (keysItr.hasNext()) {
          String k = keysItr.next();
          Object v = jsonObject.get(k);
          outputMap.put(k, v);
        }
      }
    } catch (Exception ex) {
      Logger.e(LOG_TAG, "Error while getting subscription attributes.", ex);
    }
    return outputMap;
  }

  public JSONObject getJsonObject() {
    return new JSONObject();
  }
}
