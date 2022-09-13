package com.example.cleverpush;

import static com.cleverpush.CleverPushHttpClient.BASE_URL;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.NotificationReceivedListener;
import com.example.cleverpush.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

//      String BASE_URL = "https://api-stage.cleverpush.com";
        CleverPush.getInstance(this).setApiEndpoint(BASE_URL);
//      CleverPush.getInstance(this).enableDevelopmentMode();
        CleverPush.getInstance(this).subscribe();
        CleverPush.getInstance(this).init(getString(R.string.channel_id),
                (NotificationReceivedListener)
                        result -> {
                            System.out.println("Received CleverPush Notification: " + result.getNotification().getTitle());
                        },
                result -> System.out.println("Opened CleverPush Notification: " + result.getNotification().getTitle()),
                subscriptionId -> System.out.println("CleverPush Subscription ID: " + subscriptionId));


        CleverPush.getInstance(this).requestLocationPermission();
        CleverPush.getInstance(this).initGeoFences();
        CleverPush.getInstance(this).setMaximumNotificationCount(2);

        binding.btnSubscribe.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).subscribe();
            binding.tvStatus.setText("Subscribe");
        });

        binding.btnGetnotification.setOnClickListener(view -> {
            Log.e("limit", "getNotifications :- " + CleverPush.getInstance(this).getNotifications().size());
        });

        binding.btnUnsubscribe.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).unsubscribe();
            binding.tvStatus.setText("Unsubscribe");
        });

        binding.btnGetId.setOnClickListener(view -> {
            try {
                CleverPush.getInstance(MainActivity.this).getSubscriptionId(this);
                binding.tvStatus.setText(CleverPush.getInstance(MainActivity.this).getSubscriptionId(this));
            } catch (Exception e) {
                Toast.makeText(this, "Please subscribe first", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        CleverPush.getInstance(this).hasLocationPermission();
        CleverPush.getInstance(this).initGeoFences();
    }
}