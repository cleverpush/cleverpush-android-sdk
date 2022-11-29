package com.cleverpush;

import static com.cleverpush.NotificationOpenedProcessor.processIntent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationOpenedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent(NotificationOpenedActivity.this, getIntent(), new Activity());
        if (!CleverPush.getInstance(this).isUsingNotificationOpenedCallbackListener()) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(NotificationOpenedActivity.this, getIntent(), new Activity());
        if (!CleverPush.getInstance(this).isUsingNotificationOpenedCallbackListener()) {
            finish();
        }
    }
}
