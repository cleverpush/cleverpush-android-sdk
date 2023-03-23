package com.example.cleverpush;

import static com.cleverpush.CleverPushHttpClient.BASE_URL;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.NotificationReceivedListener;
import com.example.cleverpush.databinding.ActivityMainBinding;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

//        CleverPush.getInstance(this).requestLocationPermission();
//        CleverPush.getInstance(this).initGeoFences();

        CleverPush.getInstance(this).enableDevelopmentMode();
        CleverPush.getInstance(this).setApiEndpoint(BASE_URL);
//        CleverPush.getInstance(this).subscribe();
        CleverPush.getInstance(this).init(
                getString(R.string.channel_id),
                (NotificationReceivedListener) result -> System.out.println("Received CleverPush Notification: " + result.getNotification().getTitle()),
                (NotificationOpenedListener) (result) -> {
                    System.out.println("Opened CleverPush Notification: " + result.getNotification().getUrl());

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(result.getNotification().getUrl()));
                    startActivity(browserIntent);
                },
                subscriptionId -> System.out.println("CleverPush Subscription ID: " + subscriptionId)
                );

        binding.btnSubscribe.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).subscribe();
            binding.tvStatus.setText("Subscribe");
        });

        binding.btnUnsubscribe.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).unsubscribe();
            binding.tvStatus.setText("Unsubscribe");
        });

        binding.btnTopicsDialog.setOnClickListener(view -> {
            CleverPush.getInstance(MainActivity.this).showTopicsDialog();
            binding.tvStatus.setText("ShowTopicsDialog");
        });

        binding.btnGetId.setOnClickListener(view -> {
            try {
                CleverPush.getInstance(MainActivity.this).getSubscriptionId(this);
                binding.tvStatus.setText(CleverPush.getInstance(MainActivity.this).getSubscriptionId(this));
            } catch (Exception e) {
                Toast.makeText(this, "Please subscribe first", Toast.LENGTH_SHORT).show();
            }
        });


//        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
//        browserIntent.setData(Uri.parse("cleverpush-example://test"));
//
//        startActivity(browserIntent);

//        Intent intent = new Intent(MainActivity.this, SecondViewActivity.class);
//        startActivity(intent);
    }
}