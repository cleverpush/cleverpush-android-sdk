package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.cleverpush.util.Logger;

public class NotificationOpenedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.d(LOG_TAG, "NotificationOpenedActivity onCreate");
        NotificationOpenedProcessor.processIntent(this, getIntent());
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Logger.d(LOG_TAG, "NotificationOpenedActivity onNewIntent");
        NotificationOpenedProcessor.processIntent(this, getIntent());
        finish();
    }
}
