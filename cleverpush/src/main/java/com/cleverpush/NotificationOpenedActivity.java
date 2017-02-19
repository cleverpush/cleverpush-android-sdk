package com.cleverpush;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class NotificationOpenedActivity extends Activity {

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      NotificationOpenedProcessor.processIntent(this, getIntent());
      finish();
   }

   @Override
   protected void onNewIntent(Intent intent) {
      super.onNewIntent(intent);
      NotificationOpenedProcessor.processIntent(this, getIntent());
      finish();
   }
}
