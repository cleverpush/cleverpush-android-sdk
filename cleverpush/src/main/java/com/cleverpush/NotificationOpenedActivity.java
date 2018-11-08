package com.cleverpush;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationOpenedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("CleverPush: NotificationOpenedActivity onCreate");
        super.onCreate(savedInstanceState);
        NotificationOpenedProcessor.processIntent(this, getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        System.out.println("CleverPush: NotificationOpenedActivity onNewIntent");
        super.onNewIntent(intent);
        NotificationOpenedProcessor.processIntent(this, getIntent());
        finish();
    }
}
