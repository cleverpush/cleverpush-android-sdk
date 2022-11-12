package com.example.cleverpush;

import android.app.Application;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;

import com.cleverpush.CleverPush;
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
        CleverPush.getInstance(this).init(getString(R.string.channel_id), (NotificationReceivedListener) result -> {
                    System.out.println("Received CleverPush Notification: " + result.getNotification().getTitle());
                }, result -> System.out.println("Opened CleverPush Notification: " + result.getNotification().getUrl()),
                subscriptionId -> System.out.println("CleverPush Subscription ID: " + subscriptionId));

        CleverPush.getInstance(this).init((NotificationOpenedCallbackListener) result -> {
                    final Handler handler = new Handler();
                    handler.postDelayed(() -> {
                        Logger.i("CHECKLOG", "NotificationOpenedListener L37");
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.setPackage("com.android.chrome");
                        try {
                            Logger.i("CHECKLOG", "NotificationOpenedListener Try block L42");
                            startActivity(intent);
                        } catch (ActivityNotFoundException ex) {
                            Logger.i("CHECKLOG", "NotificationOpenedListener catch block" + ex.getMessage());
                            intent.setPackage(null);
                            startActivity(intent);
                        }
                    }, 5000);
                }
        );
    }
}
