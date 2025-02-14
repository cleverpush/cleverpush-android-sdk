package com.cleverpush.responsehandlers;

import static com.cleverpush.Constants.LOG_TAG;

import android.content.Context;
import android.content.SharedPreferences;

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.util.Logger;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.util.SharedPreferencesManager;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TrackSessionStartResponseHandler {
  private final CleverPush cleverPush;
  private static String DEFAULT_DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

  public TrackSessionStartResponseHandler(CleverPush instance) {
    this.cleverPush = instance;
  }

  public CleverPushHttpClient.ResponseHandler getResponseHandler() {
    return new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        Logger.d(LOG_TAG, "Session started");
        if (response != null && !response.isEmpty()) {
          try {
            JSONObject jsonResponse = new JSONObject(response);
            String sdkForceSyncAfter = jsonResponse.optString("sdkForceSyncAfter", null);

            Date forceSyncAfter = null;
            SimpleDateFormat dateFormatter = new SimpleDateFormat(DEFAULT_DATE_TIME_FORMAT, Locale.US);
            dateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            if (sdkForceSyncAfter != null && !sdkForceSyncAfter.isEmpty()) {

              try {
                forceSyncAfter = dateFormatter.parse(sdkForceSyncAfter);
              } catch (Exception e) {
                Logger.e(LOG_TAG, "Failed to parse sdkForceSyncAfter.", e);
              }
            }

            SharedPreferences sharedPreferences = getSharedPreferences(getContext());
            SharedPreferences.Editor editor = sharedPreferences.edit();

            String subscriptionTopicLastUpdatedDateStr = sharedPreferences.getString(
                    CleverPushPreferences.SUBSCRIPTION_TOPICS_LAST_UPDATED_DATE, null);

            Date subscriptionTopicLastUpdatedDate = null;
            if (subscriptionTopicLastUpdatedDateStr != null) {
              try {
                subscriptionTopicLastUpdatedDate = dateFormatter.parse(subscriptionTopicLastUpdatedDateStr);
              } catch (Exception e) {
                Logger.e(LOG_TAG, "Failed to parse subscriptionTopicLastUpdatedDate.", e);
              }
            }

            // If no previous update date exists, use the current date
            Date currentDate = new Date();
            if (subscriptionTopicLastUpdatedDate == null) {
              subscriptionTopicLastUpdatedDate = currentDate;
            }

            // Compare dates and call checkChangedPushToken if needed
            if (forceSyncAfter != null && forceSyncAfter.after(subscriptionTopicLastUpdatedDate)) {
              cleverPush.subscribe();

              // Store the **current date and time** in SharedPreferences
              String currentDateTimeStr = dateFormatter.format(currentDate);
              editor.putString(CleverPushPreferences.SUBSCRIPTION_TOPICS_LAST_UPDATED_DATE, currentDateTimeStr);
              editor.apply();
            } else {
              Logger.d(LOG_TAG, "No need to check push token. sdkForceSyncAfter is not later than subscriptionTopicLastUpdatedDate.");
            }

          } catch (Exception e) {
            Logger.e(LOG_TAG, "TrackSessionStartResponseHandler: Failed to parse sdkForceSyncAfter.", e);
          }
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        if (throwable != null) {
          Logger.e(LOG_TAG, "Failed to track session start." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response +
                  "\nError: " + throwable.getMessage()
                  , throwable
          );
        } else {
          Logger.e(LOG_TAG, "Failed to track session start." +
                  "\nStatus code: " + statusCode +
                  "\nResponse: " + response
          );
        }
      }
    };
  }

  public SharedPreferences getSharedPreferences(Context context) {
    return SharedPreferencesManager.getSharedPreferences(context);
  }

  public Context getContext() {
    return CleverPush.context;
  }

}
