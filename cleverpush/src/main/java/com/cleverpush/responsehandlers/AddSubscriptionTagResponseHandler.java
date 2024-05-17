package com.cleverpush.responsehandlers;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.AddTagCompletedListener;
import com.cleverpush.util.Logger;
import com.cleverpush.util.SharedPreferencesManager;

import java.util.HashSet;
import java.util.Set;

public class AddSubscriptionTagResponseHandler {

  public CleverPushHttpClient.ResponseHandler getResponseHandler(String tagId,
                                                                 AddTagCompletedListener addTagCompletedListener,
                                                                 int currentPositionOfTagToAdd, Set<String> tags) {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        tags.add(tagId);
        updateSubscriptionTags(tags);
        if (addTagCompletedListener != null) {
          addTagCompletedListener.tagAdded(currentPositionOfTagToAdd);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e("CleverPush", "Error adding tag." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
                  , throwable
          );
          if (addTagCompletedListener != null) {
            addTagCompletedListener.onFailure(new Exception("Error adding tag. - HTTP " + statusCode + ": " + response + "\nError: " + throwable.getMessage()));
          }
        } else {
          Logger.e("CleverPush", "Error adding tag." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
          if (addTagCompletedListener != null) {
            Exception genericException = new Exception("Error adding tag." +
                    "\nStatus code: " + statusCode +
                    "\nResponse: " + response);
            addTagCompletedListener.onFailure(genericException);
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
    return SharedPreferencesManager.getSharedPreferences(context);
  }

  public Context getContext() {
    return CleverPush.context;
  }
}
