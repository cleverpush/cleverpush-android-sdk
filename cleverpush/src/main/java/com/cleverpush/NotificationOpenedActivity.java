package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;
import static com.cleverpush.NotificationOpenedProcessor.processIntent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.cleverpush.util.Logger;

public class NotificationOpenedActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Logger.d(LOG_TAG, "NotificationOpenedActivity onCreate");
    try {
      processIntent(NotificationOpenedActivity.this, getIntent());
      if (!CleverPush.getInstance(this).isUsingNotificationOpenedCallbackListener()) {
        finish();
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "NotificationOpenedActivity: Error in onCreate: " + e.getMessage(), e);
    }
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    Logger.d(LOG_TAG, "NotificationOpenedActivity onNewIntent");
    try {
      processIntent(NotificationOpenedActivity.this, getIntent());
      if (!CleverPush.getInstance(this).isUsingNotificationOpenedCallbackListener()) {
        finish();
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "NotificationOpenedActivity: Error in onCreate: " + e.getMessage(), e);
    }
  }

  @Override
  protected void onDestroy() {
    try {
      CleverPush.getInstance(this).setNotificationOpenedActivityDestroyedAt(System.currentTimeMillis());
    } catch (Exception e) {
      Logger.e(LOG_TAG, "NotificationOpenedActivity: Error in onDestroy: " + e.getMessage(), e);
    }
    super.onDestroy();
  }
}
