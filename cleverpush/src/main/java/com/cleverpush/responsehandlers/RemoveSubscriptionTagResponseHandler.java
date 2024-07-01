package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.RemoveTagCompletedListener;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import java.util.HashSet;
import java.util.Set;

public class RemoveSubscriptionTagResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler(String tagId,
                                                                 RemoveTagCompletedListener removeTagCompletedListener,
                                                                 int currentPositionOfTagToRemove, Set<String> tags) {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        tags.remove(tagId);
        updateSubscriptionTags(tags);
        if (removeTagCompletedListener != null) {
          removeTagCompletedListener.tagRemoved(currentPositionOfTagToRemove);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e("CleverPush", "Error removing tag." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
                  , throwable
          );
          if (removeTagCompletedListener != null) {
            removeTagCompletedListener.onFailure(new Exception("Error removing tag - HTTP " + statusCode + ": " + response + "\nError: " + throwable.getMessage()));
          }
        } else {
          Logger.e("CleverPush", "Error removing tag." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
          if (removeTagCompletedListener != null) {
            Exception genericException = new Exception("Error removing tag." +
                    "\nStatus code: " + statusCode +
                    "\nResponse: " + response);
            removeTagCompletedListener.onFailure(genericException);
          }
        }
      }
    };

  }

  public void updateSubscriptionTags(Set<String> tags) {
    SharedPreferences sharedPreferences = getSharedPreferences(getContext());
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.remove(CleverPushPreferences.SUBSCRIPTION_TAGS);
    editor.apply();
    editor.putStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, tags);
    editor.apply();
  }

  public Set<String> getSubscriptionTags() {
    return getSharedPreferences(getContext()).getStringSet(CleverPushPreferences.SUBSCRIPTION_TAGS, new HashSet<>());
  }

  public SharedPreferences getSharedPreferences(Context context) {
    return SharedPreferencesManager.getSharedPreferences(CleverPush.context);
  }

  public Context getContext() {
    return CleverPush.context;
  }
}
