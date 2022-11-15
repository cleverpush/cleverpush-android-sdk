package com.example.cleverpush;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import com.cleverpush.CleverPush;
import com.cleverpush.NotificationOpenedResult;
import com.cleverpush.listener.FinishActivity;
import com.cleverpush.listener.NotificationOpenedCallbackListener;
import com.cleverpush.listener.NotificationReceivedListener;
import com.cleverpush.util.Logger;
import com.google.firebase.FirebaseApp;

public class App extends Application {
    String url = "https://cleverpush.com/en/";

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);

        CleverPush.getInstance(this).enableDevelopmentMode();
        CleverPush.getInstance(this).subscribe();
        CleverPush.getInstance(this).init(
                getString(R.string.channel_id),
                (NotificationReceivedListener) result -> System.out.println("Received CleverPush Notification: " + result.getNotification().getTitle()),
                result -> System.out.println("Opened CleverPush Notification: " + result.getNotification().getUrl()),
                (NotificationOpenedCallbackListener) (result, finishActivity) -> {
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setPackage("com.android.chrome");
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            intent.setPackage(null);
                            startActivity(intent);
                        }
                        finishActivity.finishActivity();
                    }, 20000);
                },
                subscriptionId -> System.out.println("CleverPush Subscription ID: " + subscriptionId));
    }
}
