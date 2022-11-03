package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;
import static com.cleverpush.NotificationOpenedProcessor.processIntent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.cleverpush.listener.FinishActivity;
import com.cleverpush.util.Logger;

public class NotificationOpenedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        processIntent(NotificationOpenedActivity.this, getIntent(), new FinishActivity());
        if (!CleverPush.getInstance(this).isNotificationOpenedCallbackListenerUsed()) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(NotificationOpenedActivity.this, getIntent(),new FinishActivity());
        if (!CleverPush.getInstance(this).isNotificationOpenedCallbackListenerUsed()) {
            finish();
        }
    }
}
