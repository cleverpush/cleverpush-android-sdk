package com.cleverpush.inbox;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.preference.PreferenceManager;
import android.view.ContextThemeWrapper;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.viewpager2.widget.ViewPager2;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushHttpClient;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.Notification;
import com.cleverpush.R;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerScreens;
import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.listener.ActivityInitializedListener;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.listener.AppBannersListener;
import com.cleverpush.util.ColorUtils;
import com.cleverpush.util.CustomExceptionHandler;
import com.cleverpush.util.Logger;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class InboxDetailActivity extends AppCompatActivity {

  private static final String TAG = "CleverPush/InboxDetail";
  private final int TAB_LAYOUT_DEFAULT_HEIGHT = 48;
  private final int MAIN_LAYOUT_PADDING = 15;
  private final HandlerThread handlerThread = new HandlerThread("InboxViewModule");
  private ArrayList<Notification> notifications = new ArrayList<>();
  private int selectedPosition = 0;
  private String appBannerId = null;
  private String appBannerNotificationId = null;
  private Collection<AppBannersListener> bannersListeners = new ArrayList<>();
  private Handler handler;
  private String channel;
  private boolean loading = false;
  private Context context;
  private Collection<Banner> banners = null;
  private LinearLayout body;
  private boolean isInitialized = false;
  private Activity activity;
  private ViewPager2 viewPager2;
  private AppBannerOpenedListener openedListener;
  private Banner data;
  private View popupRoot;
  private Handler mainHandler;

  public static void launch(Activity activity, ArrayList<Notification> notifications, int selectedPosition) {
    Intent intent = new Intent(activity, InboxDetailActivity.class);
    intent.putExtra("notifications", notifications);
    intent.putExtra("selectedPosition", selectedPosition);
    activity.startActivity(intent);
  }

  public ActivityLifecycleListener getActivityLifecycleListener() {
    return ActivityLifecycleListener.getInstance();
  }

  public Handler getHandler() {
    return handler;
  }

  public boolean isLoading() {
    return loading;
  }

  public void setLoading(boolean loading) {
    this.loading = loading;
  }

  public Collection<Banner> getListOfBanners() {
    return banners;
  }

  public AppBannerOpenedListener getOpenedListener() {
    return openedListener;
  }

  public void setOpenedListener(AppBannerOpenedListener openedListener) {
    this.openedListener = openedListener;
  }

  public Collection<AppBannersListener> getBannersListeners() {
    return bannersListeners;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_inbox_detail);

    getSupportActionBar().setTitle("Message Detail");
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    this.context = this;
    this.activity = this;
    init();
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  private void init() {
    int layoutId = R.layout.activity_inbox_detail;
    popupRoot = createLayout(layoutId);
    handleBundleData(getIntent().getExtras());
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
    mainHandler = new CustomExceptionHandler(activity.getMainLooper());
  }

  private View createLayout(int layoutId) {
    View layout = null;
    try {
      layout = activity.getLayoutInflater().inflate(layoutId, null);
      layout.setOnClickListener(view -> dismiss());
      return layout;
    } catch (InflateException inflateException) {
      try {
        int themeId = R.style.cleverpush_app_banner_theme;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(activity, themeId);

        LayoutInflater inflater = LayoutInflater.from(contextThemeWrapper);
        layout = inflater.inflate(layoutId, null);
        layout.setOnClickListener(view -> dismiss());
        return layout;
      } catch (Exception exception) {
        Logger.e(TAG, "InflateException: " + exception.getLocalizedMessage());
      }
    } catch (Exception exception) {
      Logger.e(TAG, exception.getLocalizedMessage());
    }
    return null;
  }

  private void handleBundleData(Bundle extras) {
    if (extras != null) {
      if (extras.containsKey("selectedPosition")) {
        selectedPosition = extras.getInt("selectedPosition");
      }
      if (extras.containsKey("notifications")) {
        notifications = (ArrayList<Notification>) extras.getSerializable("notifications");
        Notification clickedNotification = notifications.get(selectedPosition);
        appBannerId = clickedNotification.getInboxAppBanner();
        appBannerNotificationId = clickedNotification.getId();
        showBannerById(appBannerId, appBannerNotificationId);
      }
    }
  }

  private void loadInboxDetails() {
    body = findViewById(R.id.inboxViewBannerBody);
    ImageView bannerBackGroundImage = findViewById(R.id.inboxViewBannerBackgroundImage);
    ConstraintLayout parent = findViewById(R.id.parent);

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        composeBackground(parent, body);
      }
    });

    isInitialized = true;

    show();

    setOpenedListener(action -> {
//            sendBannerEvent("clicked", bannerPopup.getData());

      if (getCleverPushInstance().getAppBannerOpenedListener() != null) {
        getCleverPushInstance().getAppBannerOpenedListener().opened(action);
      }

      if (action.getType().equals("subscribe")) {
        getCleverPushInstance().subscribe();
      }

      if (action.getType().equals("addTags")) {
        getCleverPushInstance().addSubscriptionTags(action.getTags().toArray(new String[0]));
      }

      if (action.getType().equals("removeTags")) {
        getCleverPushInstance().removeSubscriptionTags(action.getTags().toArray(new String[0]));
      }

      if (action.getType().equals("addTopics")) {
        Set<String> topics = getCleverPushInstance().getSubscriptionTopics();
        topics.addAll(action.getTopics());
        getCleverPushInstance().setSubscriptionTopics(topics.toArray(new String[0]));
      }

      if (action.getType().equals("removeTopics")) {
        Set<String> topics = getCleverPushInstance().getSubscriptionTopics();
        topics.removeAll(action.getTopics());
        getCleverPushInstance().setSubscriptionTopics(topics.toArray(new String[0]));
      }

      if (action.getType().equals("setAttribute")) {
        getCleverPushInstance().setSubscriptionAttribute(action.getAttributeId(), action.getAttributeValue());
      }

      if (action.getType().equals("switchScreen")) {
      }
    });
  }

  public void show() {
    if (!isInitialized) {
      throw new IllegalStateException("Must be initialized");
    }
    new tryShowSafe().execute();
  }

  private void displayBanner(LinearLayout body) {
    // We cant move this code to composeHtmlBanner, because it wont work then anymore
    if (data.getContentType() != null && data.getContentType().equalsIgnoreCase(Banner.CONTENT_TYPE_HTML)) {
      body.setPadding(0, 0, 0, 0);
    }

    if (!data.isCarouselEnabled() && !data.getEnableMultipleScreens()) {
      data.getScreens().clear();
      BannerScreens bannerScreens = new BannerScreens();
      bannerScreens.setBlocks(data.getBlocks());
      data.getScreens().add(bannerScreens);
    }
    setUpBannerBlocks(data);

    ConstraintLayout mConstraintLayout = (ConstraintLayout) popupRoot;
    ConstraintSet mConstraintSet = new ConstraintSet();
    mConstraintSet.clone(mConstraintLayout);

    if (data.isMarginEnabled()) {
      mConstraintSet.constrainPercentWidth(R.id.inboxViewFrameLayout, 0.9f);
      mConstraintSet.constrainPercentHeight(R.id.inboxViewFrameLayout, 0.9f);
    } else {
      mConstraintSet.constrainPercentWidth(R.id.inboxViewFrameLayout, 1.0f);
      mConstraintSet.constrainPercentHeight(R.id.inboxViewFrameLayout, 1.0f);
    }

    mConstraintSet.constrainHeight(R.id.inboxViewFrameLayout, ConstraintSet.MATCH_CONSTRAINT);
    mConstraintSet.applyTo(mConstraintLayout);

    body.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
  }

  private void runInMain(Runnable runnable) {
    if (isRootReady()) {
      mainHandler.post(runnable);
    }
  }

  private void runInMain(Runnable runnable, long delay) {
    if (delay <= 0L) {
      mainHandler.post(runnable);
    } else {
      mainHandler.postDelayed(runnable, delay);
    }
  }

  private void setUpBannerBlocks(Banner data) {
    viewPager2 = findViewById(R.id.inboxViewCarousel_pager);
    TabLayout tabLayout = findViewById(R.id.inboxViewCarousel_pager_tab_layout);

    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
    viewPager2.setLayoutParams(layoutParams);

    InboxDetailBannerCarouselAdapter appBannerCarouselAdapter = new InboxDetailBannerCarouselAdapter(activity, data, this, openedListener);
    viewPager2.setAdapter(appBannerCarouselAdapter);
    if (data.getScreens().size() > 1) {
      if (!data.getEnableMultipleScreens() || !data.isCarouselEnabled()) {
        viewPager2.setUserInputEnabled(false);
        tabLayout.setVisibility(View.GONE);
      } else {
        tabLayout.setVisibility(View.VISIBLE);
        viewPager2.setUserInputEnabled(true);
      }
      new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {
      }).attach();
    }
  }

  private void updatePagerHeightForChild(View view, ViewPager2 viewPager2) {
    view.post(() -> {
      int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(view.getWidth(), View.MeasureSpec.EXACTLY);
      int hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
      LinearLayout.LayoutParams layoutParams;
      view.measure(wMeasureSpec, hMeasureSpec);
      if (popupRoot.getMeasuredHeight() > view.getMeasuredHeight() + TAB_LAYOUT_DEFAULT_HEIGHT + MAIN_LAYOUT_PADDING) {
        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, view.getMeasuredHeight());
      } else {
        int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.77);
        layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
      }

      viewPager2.setLayoutParams(layoutParams);
      viewPager2.invalidate();
    });
  }

  public void dismiss() {
    if (!isInitialized) {
      Logger.e(TAG, "Must be initialized");
      return;
    }

    this.toggleShowing(false);
    finish();

  }

  private boolean isRootReady() {
    return activity.getWindow().getDecorView().isShown();
  }

  private View getRoot() {
    return activity.getWindow().getDecorView().getRootView();
  }

  public void showBannerById(String bannerId, String notificationId) {
    getActivityLifecycleListener().setActivityInitializedListener(new ActivityInitializedListener() {
      @Override
      public void initialized() {
        Logger.d(TAG, "InboxDetailBannerById: " + bannerId);
        getBanners(banners -> {
          for (Banner banner : banners) {
            if (banner.getId().equals(bannerId)) {
              Logger.d(TAG, banner.getId());
              data = banner;
              loadInboxDetails();
              break;
            }
          }
        }, notificationId);
      }
    });
  }

  public void getBanners(AppBannersListener listener, String notificationId) {
    if (listener == null) {
      return;
    }
    CleverPush cleverPush = getCleverPushInstance();
    channel = cleverPush.getChannelId(cleverPush.getContext());
    if (notificationId != null) {
      // reload banners because the banner might have been created just seconds agox
      bannersListeners.add(listener);

      getHandler().post(() -> {
        this.loadBanners(notificationId, channel);
      });
    } else {
      if (getListOfBanners() == null) {
        bannersListeners.add(listener);
      } else {
        listener.ready(getListOfBanners());
      }
    }
  }

  void loadBanners(String notificationId, String channelId) {
    if (isLoading()) {
      return;
    }

    setLoading(true);
    String bannersPath = "/channel/" + channelId + "/app-banners?platformName=Android";
    if (getCleverPushInstance().isDevelopmentModeEnabled()) {
      bannersPath += "&t=" + System.currentTimeMillis();
    }
    if (notificationId != null && !notificationId.isEmpty()) {
      bannersPath += "&notificationId=" + notificationId;
    }
    CleverPushHttpClient.get(bannersPath, new CleverPushHttpClient.ResponseHandler() {
      @Override
      public void onSuccess(String response) {
        setLoading(false);
        banners = new LinkedList<>();
        try {
          JSONObject responseJson = new JSONObject(response);
          JSONArray rawBanners = responseJson.getJSONArray("banners");

          for (int i = 0; i < rawBanners.length(); ++i) {
            JSONObject rawBanner = rawBanners.getJSONObject(i);
            Banner banner = Banner.create(rawBanner);
            banners.add(banner);
          }

          for (AppBannersListener listener : getBannersListeners()) {
            listener.ready(banners);
          }

          bannersListeners = new ArrayList<>();
        } catch (Exception ex) {
          Logger.e(TAG, ex.getMessage(), ex);
        }
      }

      @Override
      public void onFailure(int statusCode, String response, Throwable throwable) {
        setLoading(false);
        Logger.e(TAG, "Something went wrong when loading inbox view banner." + "\nStatus code: " + statusCode + "\nResponse: " + response + "\nError: " + throwable.getMessage());
      }
    });
  }

  public CleverPush getCleverPushInstance() {
    if (context != null) {
      return CleverPush.getInstance(context);
    }
    return null;
  }

  private void composeBackground(ConstraintLayout bannerBackground, LinearLayout body) {
    BannerBackground bg = data.getBackground();
    final ViewTreeObserver observer = body.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, body.getHeight());
        bannerBackground.setLayoutParams(layoutParams);
        if (observer.isAlive()) {
          observer.removeGlobalOnLayoutListener(this);
        }
      }
    });

    if (bg.getImageUrl() == null || bg.getImageUrl().equalsIgnoreCase("null") || bg.getImageUrl().equalsIgnoreCase("")) {
      GradientDrawable drawableBG = new GradientDrawable();
      drawableBG.setShape(GradientDrawable.RECTANGLE);

      if (data.isDarkModeEnabled(this) && bg.getDarkColor() != null) {
        drawableBG.setColor(ColorUtils.parseColor(bg.getDarkColor()));
      } else if (bg.getColor() != null) {
        drawableBG.setColor(ColorUtils.parseColor(bg.getColor()));
      } else {
        drawableBG.setColor(Color.WHITE);
      }
      bannerBackground.setBackground(drawableBG);
    } else if (bg.getImageUrl() != null) {
      new Thread(() -> {
        try {
          String imageUrl;
          if (data.isDarkModeEnabled(this) && bg.getDarkImageUrl() != null) {
            imageUrl = bg.getDarkImageUrl();
          } else {
            imageUrl = bg.getImageUrl();
          }

          InputStream in = new URL(imageUrl).openStream();
          Bitmap bitmap = BitmapFactory.decodeStream(in);
          if (bitmap != null) {
            Drawable drawable = new BitmapDrawable(bitmap);
            bannerBackground.setBackgroundDrawable(drawable);
//                        bannerBackground.set(bitmap);
          }
        } catch (Exception ignored) {
          Logger.e(TAG, ignored.getLocalizedMessage());
        }
      }).start();
    } else {
      bannerBackground.setVisibility(View.GONE);
    }
  }

  private float getPXScale() {
    int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    return Math.max(Math.min(screenWidth / 400.0f, 10f), 1.0f);
  }

  private void toggleShowing(boolean isShowing) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(CleverPushPreferences.APP_BANNER_SHOWING, isShowing);
    editor.commit();
  }

  public void moveToNextScreen() {
    int currentPosition = viewPager2.getCurrentItem();
    if (currentPosition < data.getScreens().size() - 1) {
      viewPager2.setCurrentItem(currentPosition + 1);
    }
  }

  public void moveToNextScreen(int screenPosition) {
    viewPager2.setCurrentItem(screenPosition, true);
  }

  public Banner getData() {
    return data;
  }

  public ViewPager2 getViewPager2() {
    return viewPager2;
  }

  @Override
  public boolean onSupportNavigateUp() {
    finish();
    return true;
  }

  public class tryShowSafe extends AsyncTask<String, Void, Boolean> {

    @Override
    protected Boolean doInBackground(String... strings) {
      if (isRootReady()) {
        return true;
      } else {
        new Timer().schedule(new TimerTask() {
          @Override
          public void run() {
            new tryShowSafe().execute();
          }
        }, 100);
      }
      return false;
    }

    @Override
    protected void onPostExecute(Boolean isRootReady) {
      super.onPostExecute(isRootReady);
      if (isRootReady) {
        try {
          displayBanner(body);
        } catch (Exception e) {
          Logger.e(TAG, e.getLocalizedMessage());
        }
      }
    }
  }

}