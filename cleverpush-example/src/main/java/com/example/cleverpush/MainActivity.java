package com.example.cleverpush;

import static com.cleverpush.CleverPushHttpClient.BASE_URL;
import static com.cleverpush.Constants.LOG_TAG;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.cleverpush.CleverPush;
import com.cleverpush.banner.WebViewActivity;
import com.cleverpush.listener.NotificationOpenedListener;
import com.cleverpush.listener.NotificationReceivedListener;
import com.cleverpush.util.Logger;
import com.example.cleverpush.databinding.ActivityMainBinding;

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CleverPush.getInstance(this).enableDevelopmentMode();
        CleverPush.getInstance(this).setApiEndpoint(BASE_URL);
        CleverPush.getInstance(this).subscribe();
        CleverPush.getInstance(this).init(
                getString(R.string.channel_id),
                (NotificationReceivedListener) result -> System.out.println("Received CleverPush Notification: " + result.getNotification().getTitle()),
                (NotificationOpenedListener) (result) -> {
                    System.out.println("Opened CleverPush Notification: " + result.getNotification().getUrl());
                },
                subscriptionId -> System.out.println("CleverPush Subscription ID: " + subscriptionId)
                );

        setupBasicButtons();
    }

    void setupBasicButtons() {
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

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
    }

    void testStoryViewTrackUrlOpenedListener() {
        /*binding.storyView.setOpenedListener((webResourceRequest) -> {
            Logger.d(LOG_TAG, "StoryView URl: " + webResourceRequest.getUrl());
            Logger.d(LOG_TAG, "StoryView method: " + webResourceRequest.getMethod());
        });*/
    }

    void testAppBannerTrackClickOpenedListener() {
        CleverPush.getInstance(this).setAppBannerOpenedListener((bannerAction) -> {
            Logger.d(LOG_TAG, "AppBannerOpened " + bannerAction.getType());
            Map<String, Object> map = bannerAction.getCustomData();
            if (map != null) {
                Logger.d(LOG_TAG, "AppBannerOpened " + map.keySet());
            }
        });
    }

    void miscFunctions() {
        CleverPush.getInstance(this).requestLocationPermission();
        CleverPush.getInstance(this).initGeoFences();
        WebViewActivity.launch(this, "https://www.google.de");
    }

    void getBannersByCategory() {
        /*binding.btnGetBannerCategory.setOnClickListener(view -> {
            String categoryId = "testCategoryId";
            CleverPush.getInstance(this).getAppBannersByGroup((Collection<Banner> banners) -> {
                        for (Banner banner : banners) {
                            Logger.d(LOG_TAG, banner.getId());
                        }
                    },
                    categoryId);
            binding.tvStatus.setText("Got banners");
        });*/
    }
}
