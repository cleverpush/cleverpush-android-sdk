package com.cleverpush.responsehandlers;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.listener.InitializeListener;
import com.cleverpush.listener.UnsubscribedListener;
import com.cleverpush.util.Logger;

public class UnsubscribeResponseHandler {

  private final CleverPush cleverPush;
  private final UnsubscribedListener listener;
  private final InitializeListener initializeListener;

  public UnsubscribeResponseHandler(CleverPush cleverPush, UnsubscribedListener listener, InitializeListener initializeListener) {
    this.cleverPush = cleverPush;
    this.listener = listener;
    this.initializeListener = initializeListener;
  }

  public CleverPushHttpClient.ResponseHandler getResponseHandler() {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        try {
          Logger.d("CleverPush", "unsubscribe success");
          cleverPush.clearSubscriptionData();

          SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
          SharedPreferences.Editor editor = sharedPreferences.edit();
          editor.putBoolean(CleverPushPreferences.UNSUBSCRIBED, true);
          editor.commit();

          if (listener != null) {
            listener.onSuccess();
          }
        } catch (Throwable throwable) {
          Logger.e("CleverPush", "Error", throwable);

          if (listener != null) {
            listener.onFailure(throwable);
          }
        }

        if (initializeListener != null) {
          initializeListener.onInitialized();
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        Logger.e("CleverPush", "Failed while unsubscribe request - " + statusCode + " - " + response, throwable);
        if (initializeListener != null) {
          initializeListener.onInitialized();
        }

        if (listener != null) {
          listener.onFailure(throwable);
        }
      }
    };
  }
}
