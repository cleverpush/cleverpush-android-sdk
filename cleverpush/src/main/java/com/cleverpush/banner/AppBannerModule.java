package com.cleverpush.banner;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.View;

import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerDismissType;
import com.cleverpush.banner.models.BannerFrequency;
import com.cleverpush.banner.models.BannerStatus;
import com.cleverpush.banner.models.BannerStopAtType;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class AppBannerModule {
    @FunctionalInterface
    private interface OnBannerLoaded {
        void notify(List<Banner> banners);
    }


    private static final String TAG = "CleverPush/AppBanner";
    private static final String APP_BANNER_SHARED_PREFS = "com.cleverpush.appbanner";
    private static final String SHOWN_APP_BANNER_PREF = "shownAppBanners";

    private static AppBannerModule instance;

    public static void init(Activity activity, String channel) {
        init(activity, channel, false);
    }

    public static void init(Activity activity, String channel, boolean showDrafts) {
        if(instance == null) {
            instance = new AppBannerModule(activity, channel, showDrafts);
        } else {
            throw new IllegalStateException("Already initialized");
        }
    }

    private Activity activity;
    private String channel;
    private boolean showDrafts;

    private List<AppBannerPopup> popups = new ArrayList<>();

    private HandlerThread handlerThread = new HandlerThread("AppBannerModule");
    private Handler handler;

    private View getRoot() {
        return activity.getWindow().getDecorView().getRootView();
    }

    private AppBannerModule(Activity activity, String channel, boolean showDrafts) {
        this.activity = activity;
        this.channel = channel;
        this.showDrafts = showDrafts;

        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());
        handler.post(() -> loadBanners(this::startup));
    }

    private void loadBanners(OnBannerLoaded cb) {
        CleverPushHttpClient.get("/channel/" + channel + "/app-banners", new CleverPushHttpClient.ResponseHandler() {
            @Override
            public void onSuccess(String response) {
                List<Banner> banners = new LinkedList<>();
                try {
                    JSONObject responseJson = new JSONObject(response);
                    JSONArray rawBanners = responseJson.getJSONArray("banners");

                    for(int i = 0; i < rawBanners.length(); ++i) {
                        JSONObject rawBanner = rawBanners.getJSONObject(i);
                        Banner banner = Banner.create(rawBanner);

                        banners.add(banner);
                    }

                    cb.notify(banners);
                } catch (Exception ex) {
                    Log.e(TAG, ex.getMessage(), ex);
                }
            }

            @Override
            public void onFailure(int statusCode, String response, Throwable throwable) {
                Log.e(TAG, "Something went wrong when loading banners." +
                        "\nStatus code: " + statusCode +
                        "\nResponse: " + response
                );
            }
        });
    }

    private void startup(List<Banner> banners) {
        createBanners(banners);
        scheduleBanners();
    }

    private void createBanners(List<Banner> banners) {
        for(Banner banner: banners) {
            if(banner.getStatus() == BannerStatus.Draft && !showDrafts) {
                continue;
            }

            if(banner.getFrequency() == BannerFrequency.Once && isBannerShown(banner.getId())) {
                continue;
            }

            Date now = new Date();
            if(banner.getStopAtType() == BannerStopAtType.SpecificTime && banner.getStopAt().after(now)) {
                continue;
            }

            popups.add(new AppBannerPopup(activity, banner));
        }
    }

    private void scheduleBanners() {
        Date now = new Date();
        for(AppBannerPopup bannerPopup: popups) {
            Banner banner = bannerPopup.getData();

            if(banner.getStartAt().before(now)) {
                handler.post(() -> showBanner(bannerPopup));
            } else {
                long delay = banner.getStartAt().getTime() - now.getTime();
                handler.postDelayed(() -> showBanner(bannerPopup), delay);
            }
        }
    }

    private void showBanner(AppBannerPopup bannerPopup) {
        bannerPopup.init();
        bannerPopup.show();

        if(bannerPopup.getData().getFrequency() == BannerFrequency.Once) {
            bannerIsShown(bannerPopup.getData().getId());
        }

        if(bannerPopup.getData().getDismissType() == BannerDismissType.Timeout) {
            long timeout = Math.max(0, bannerPopup.getData().getDismissTimeout());
            handler.postDelayed(bannerPopup::dismiss, timeout * 1000);
        }
    }

    private boolean isBannerShown(String id) {
        SharedPreferences sharedPreferences = this.activity.getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
        Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

        assert shownBanners != null;
        return shownBanners.contains(id);
    }

    private void bannerIsShown(String id) {
        SharedPreferences sharedPreferences = this.activity.getSharedPreferences(APP_BANNER_SHARED_PREFS, Context.MODE_PRIVATE);
        Set<String> shownBanners = sharedPreferences.getStringSet(SHOWN_APP_BANNER_PREF, new HashSet<>());

        assert shownBanners != null;
        shownBanners.add(id);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(SHOWN_APP_BANNER_PREF).apply();
        editor.putStringSet(SHOWN_APP_BANNER_PREF, shownBanners);
        editor.commit();
    }

    @Override
    protected void finalize() throws Throwable {
        handlerThread.quit();
        super.finalize();
    }
}
