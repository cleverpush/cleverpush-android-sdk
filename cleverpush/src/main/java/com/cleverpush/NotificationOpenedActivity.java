package com.cleverpush;

import static com.cleverpush.NotificationOpenedProcessor.processIntent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationOpenedActivity extends Activity {

    public static NotificationOpenedActivity notificationOpenActivity = null;

    void setNotificationOpenActivity(NotificationOpenedActivity activity) {
        if (NotificationOpenedActivity.notificationOpenActivity !=  null) {
            return;
        }
        NotificationOpenedActivity.notificationOpenActivity = activity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setNotificationOpenActivity(this);
        processIntent(NotificationOpenedActivity.this, getIntent());
        if (!CleverPush.getInstance(this).isUsingNotificationOpenedCallbackListener()) {
            finish();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        processIntent(NotificationOpenedActivity.this, getIntent());
        if (!CleverPush.getInstance(this).isUsingNotificationOpenedCallbackListener()) {
            finish();
        }
    }
}
