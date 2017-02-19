package com.cleverpush;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.cleverpush.listener.NotificationOpenedListener;

public class CleverPush {

    private static CleverPush instance;

    public static CleverPush getInstance(@NonNull Context context) {
        if (instance == null) {
            instance = new CleverPush(context);
        }
        return instance;
    }

    private Context context;

    private NotificationOpenedListener notificationOpenedListener;

    private CleverPush(@NonNull Context context) {
        if (context instanceof Application) {
            this.context = context;
        } else {
            this.context = context.getApplicationContext();
        }
    }

    public void init() throws Exception {
        init(null);
    }

    public void init(@Nullable final NotificationOpenedListener notificationOpenedListener) throws Exception {
        String channelId = MetaDataUtils.getChannelId(this.context);
        if (channelId == null) {
            throw new Exception("Please set up your CLEVERPUSH_CHANNEL_ID in AndroidManifest.xml");
        }

        this.notificationOpenedListener = notificationOpenedListener;
    }

    public void fireNotificationOpenedListener(final NotificationOpenedResult openedResult) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread())
            notificationOpenedListener.notificationOpened(openedResult);
        else {
            ((Activity) this.context).runOnUiThread(() -> notificationOpenedListener.notificationOpened(openedResult));
        }
    }
}
