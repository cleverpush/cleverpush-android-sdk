package com.cleverpush;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationOpenedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NotificationOpenedProcessor.processIntent(context, intent);
    }
}
