package com.example.cleverpush;

import static com.cleverpush.CleverPushHttpClient.BASE_URL;

import android.content.ActivityNotFoundException;
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

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    String url = "https://cleverpush.com/en/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        CleverPush.getInstance(this).requestLocationPermission();
        CleverPush.getInstance(this).initGeoFences();

        CleverPush.getInstance(this).enableDevelopmentMode();
        CleverPush.getInstance(this).setApiEndpoint(BASE_URL);
        CleverPush.getInstance(this).subscribe();
        CleverPush.getInstance(this).init(getString(R.string.channel_id), (NotificationReceivedListener) result -> {
                    System.out.println("Received CleverPush Notification: " + result.getNotification().getTitle());
                }, result -> System.out.println("Opened CleverPush Notification: " + result.getNotification().getUrl()),
                subscriptionId -> System.out.println("CleverPush Subscription ID: " + subscriptionId));

        CleverPush.getInstance(this).init((NotificationOpenedListener) result -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setPackage("com.android.chrome");
            try {
                startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                intent.setPackage(null);
                startActivity(intent);
            }
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

    }
}
