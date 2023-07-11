package com.cleverpush.banner;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;

import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;
import androidx.viewpager2.widget.ViewPager2;

import com.cleverpush.ActivityLifecycleListener;
import com.cleverpush.util.Logger;
import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerScreens;
import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.util.ColorUtils;
import com.cleverpush.util.CustomExceptionHandler;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.InputStream;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

public class AppBannerPopup {

  private static final String POSITION_TYPE_TOP = "top";
  private static final String POSITION_TYPE_BOTTOM = "bottom";
  private static final String POSITION_TYPE_FULL = "full";
  private static final String TAG = "CleverPush/AppBanner";

  private final int TAB_LAYOUT_DEFAULT_HEIGHT = 48;
  private final int MAIN_LAYOUT_PADDING = 15;
  private final Handler mainHandler;
  private final Activity activity;
  private final Banner data;
  private PopupWindow popup;
  private View popupRoot;
  private ViewPager2 viewPager2;
  private LinearLayout body;
  private int currentStatusBarColor;
  private int currentNavigationBarColor;

  private AppBannerOpenedListener openedListener;

  private boolean isInitialized = false;

  AppBannerPopup(Activity activity, Banner data) {
    this.activity = activity;
    this.data = data;

    mainHandler = new CustomExceptionHandler(activity.getMainLooper());
  }

  private static SpringForce getDefaultForce(float finalValue) {
    SpringForce force = new SpringForce(finalValue);
    force.setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
    float DEFAULT_STIFFNESS_RATIO = 300f;
    force.setStiffness(DEFAULT_STIFFNESS_RATIO);

    return force;
  }

  private boolean isRootReady() {
    return activity.getWindow().getDecorView().isShown();
  }

  private View getRoot() {
    return activity.getWindow().getDecorView().getRootView();
  }

  private float getPXScale() {
    int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;

    return Math.max(Math.min(screenWidth / 400.0f, 10f), 1.0f);
  }

  public Banner getData() {
    return data;
  }

  public boolean isHTMLBanner() {
    return data.getContentType() != null && data.getContentType().equalsIgnoreCase(Banner.CONTENT_TYPE_HTML);
  }

  public ViewPager2 getViewPager2() {
    return viewPager2;
  }

  public void init() {
    if (isInitialized) {
      return;
    }

    int layoutId = R.layout.app_banner;

    popupRoot = createLayout(layoutId);
    if (popupRoot == null) {
      return;
    }

    body = popupRoot.findViewById(R.id.bannerBody);
    ImageView bannerBackGroundImage = popupRoot.findViewById(R.id.bannerBackgroundImage);

    popup = new PopupWindow(
        popupRoot,
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
        true
    );

    if (isHTMLBanner()) {
      setNotchColor(false);
      FrameLayout frameLayout = popupRoot.findViewById(R.id.frameLayout);
      ConstraintLayout parent = popupRoot.findViewById(R.id.parent);

      parent.setBackgroundColor(Color.TRANSPARENT);
      frameLayout.setBackgroundColor(Color.TRANSPARENT);
      body.setBackgroundColor(Color.TRANSPARENT);
      bannerBackGroundImage.setVisibility(View.GONE);
    } else {
      bannerBackGroundImage.setVisibility(View.VISIBLE);
      composeBackground(bannerBackGroundImage, body);
    }
    
    popup.setAnimationStyle(R.style.banner_animation);
    popup.setOnDismissListener(() -> toggleShowing(false));

    isInitialized = true;
  }

  private void toggleShowing(boolean isShowing) {
    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(CleverPush.context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(CleverPushPreferences.APP_BANNER_SHOWING, isShowing);
    editor.commit();
  }

  public void show() {
    if (!isInitialized) {
      throw new IllegalStateException("Must be initialized");
    }
    new tryShowSafe().execute();
  }

  public void dismiss() {
    if (!isInitialized) {
      Logger.e(TAG, "Must be initialized");
      return;
    }

    runInMain(() -> animateBody(0f, getRoot().getHeight()));
    runInMain(() -> {
      popup.dismiss();
      if (isHTMLBanner()) {
        setNotchColor(true);
      }
      this.toggleShowing(false);
      CleverPush.getInstance(CleverPush.context).getAppBannerModule().onAppBannerDismiss();
    }, 200);

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

  public AppBannerOpenedListener getOpenedListener() {
    return openedListener;
  }

  public void setOpenedListener(AppBannerOpenedListener openedListener) {
    this.openedListener = openedListener;
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

  private void composeBackground(ImageView bannerBackground, LinearLayout body) {
    BannerBackground bg = data.getBackground();
    final ViewTreeObserver observer = body.getViewTreeObserver();
    observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        FrameLayout.LayoutParams layoutParams =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, body.getHeight());
        bannerBackground.setLayoutParams(layoutParams);
        if (observer.isAlive()) {
          observer.removeGlobalOnLayoutListener(this);
        }
      }
    });

    if (bg.getImageUrl() == null || bg.getImageUrl().equalsIgnoreCase("null") || bg.getImageUrl()
        .equalsIgnoreCase("")) {
      GradientDrawable drawableBG = new GradientDrawable();
      drawableBG.setShape(GradientDrawable.RECTANGLE);
      if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
        drawableBG.setCornerRadius(10 * getPXScale());
      }

      if (data.isDarkModeEnabled(activity) && bg.getDarkColor() != null) {
        drawableBG.setColor(ColorUtils.parseColor(bg.getDarkColor()));
      } else if (bg.getColor() != null) {
        drawableBG.setColor(ColorUtils.parseColor(bg.getColor()));
      } else {
        drawableBG.setColor(Color.WHITE);
      }
      if (isHTMLBanner()) {
        drawableBG.setColor(Color.TRANSPARENT);
      }
      bannerBackground.setBackground(drawableBG);
    } else if (bg.getImageUrl() != null) {
      new Thread(() -> {
        try {
          String imageUrl;
          if (data.isDarkModeEnabled(activity) && bg.getDarkImageUrl() != null) {
            imageUrl = bg.getDarkImageUrl();
          } else {
            imageUrl = bg.getImageUrl();
          }

          InputStream in = new URL(imageUrl).openStream();
          Bitmap bitmap = BitmapFactory.decodeStream(in);
          if (bitmap != null) {
            bannerBackground.setImageBitmap(bitmap);
          }
        } catch (Exception ignored) {
          Logger.e(TAG, ignored.getLocalizedMessage());
        }
      }).start();
    } else {
      bannerBackground.setVisibility(View.GONE);
    }
  }

  private void animateBody(float from, float to) {
    View bannerBody = popup.getContentView().findViewById(R.id.bannerBody);
    bannerBody.setTranslationY(from);

    SpringAnimation springInPopup = new SpringAnimation(bannerBody, DynamicAnimation.TRANSLATION_Y);
    springInPopup.setSpring(getDefaultForce(to));

    springInPopup.start();
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

  private void displayBanner(LinearLayout body) {
    // We cant move this code to composeHtmlBanner, because it wont work then anymore
    if (isHTMLBanner()) {
      body.setPadding(0, 0, 0, 0);
    }

    if (!data.isCarouselEnabled() && !data.getEnableMultipleScreens()) {
      data.getScreens().clear();
      BannerScreens bannerScreens = new BannerScreens();
      bannerScreens.setBlocks(data.getBlocks());
      data.getScreens().add(bannerScreens);
    }
    setUpBannerBlocks();

    if (data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
      ConstraintLayout mConstraintLayout = (ConstraintLayout) popupRoot;
      ConstraintSet mConstraintSet = new ConstraintSet();
      mConstraintSet.clone(mConstraintLayout);

      if (data.isMarginEnabled()) {
        mConstraintSet.constrainPercentWidth(R.id.frameLayout, 0.9f);
        mConstraintSet.constrainPercentHeight(R.id.frameLayout, 0.9f);
      } else {
        mConstraintSet.constrainPercentWidth(R.id.frameLayout, 1.0f);
        mConstraintSet.constrainPercentHeight(R.id.frameLayout, 1.0f);
      }

      mConstraintSet.constrainHeight(R.id.frameLayout, ConstraintSet.MATCH_CONSTRAINT);
      mConstraintSet.applyTo(mConstraintLayout);

      body.setLayoutParams(
          new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
      runInMain(() -> popup.showAtLocation(popupRoot, Gravity.TOP, 0, 0));
    } else {
      body.setLayoutParams(
          new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      ConstraintLayout mConstraintLayout = (ConstraintLayout) popupRoot;
      ConstraintSet mConstraintSet = new ConstraintSet();

      switch (data.getPositionType()) {
        case POSITION_TYPE_TOP:
          mConstraintSet.clone(mConstraintLayout);
          mConstraintSet.clear(R.id.frameLayout, ConstraintSet.TOP);
          mConstraintSet.clear(R.id.frameLayout, ConstraintSet.BOTTOM);
          mConstraintSet.connect(R.id.frameLayout, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 40);
          mConstraintSet.applyTo(mConstraintLayout);

          runInMain(() -> popup.showAtLocation(popupRoot, Gravity.TOP, 0, 0));
          break;
        case POSITION_TYPE_BOTTOM:
          mConstraintSet.clone(mConstraintLayout);
          mConstraintSet.clear(R.id.frameLayout, ConstraintSet.TOP);
          mConstraintSet.clear(R.id.frameLayout, ConstraintSet.BOTTOM);
          mConstraintSet.connect(R.id.frameLayout, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
              40);
          mConstraintSet.applyTo(mConstraintLayout);

          runInMain(() -> popup.showAtLocation(popupRoot, Gravity.BOTTOM, 0, 0));
          break;
        default:
          runInMain(() -> popup.showAtLocation(popupRoot, Gravity.CENTER, 0, 0));
          break;
      }
    }
  }

  private void setUpBannerBlocks() {
    viewPager2 = popupRoot.findViewById(R.id.carousel_pager);
    TabLayout tabLayout = popupRoot.findViewById(R.id.carousel_pager_tab_layout);

    if (data.isCloseButtonEnabled()) {
      ImageButton buttonClose;
      if (data.isCloseButtonPositionStaticEnabled()) {
        buttonClose = popupRoot.findViewById(R.id.buttonClose);
      } else {
        buttonClose = popupRoot.findViewById(R.id.buttonCloseOverlap);
      }
      buttonClose.setVisibility(View.VISIBLE);
      buttonClose.setOnClickListener(view -> dismiss());
    }

    if (data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
      LinearLayout.LayoutParams layoutParams =
          new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
      viewPager2.setLayoutParams(layoutParams);
    }

    AppBannerCarouselAdapter appBannerCarouselAdapter =
        new AppBannerCarouselAdapter(activity, data, this, openedListener);
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

    viewPager2.setPageTransformer((page, position) -> {
      if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
        updatePagerHeightForChild(page, viewPager2);
      }
    });

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
          animateBody(getRoot().getHeight(), 0f);
        } catch (Exception e) {
          Logger.e(TAG, e.getLocalizedMessage());
        }
      }
    }
  }

  private void setNotchColor(boolean isFinish) {
    try {
      final String notchColor;
      BannerBackground bg = data.getBackground();
      if (data.isDarkModeEnabled(activity) && bg.getDarkColor() != null) {
        notchColor = bg.getDarkColor();
      } else if (bg.getColor() != null) {
        notchColor = bg.getColor();
      } else {
        notchColor = null;
      }

      if (notchColor != null && notchColor.length() > 0) {
        Window window = ActivityLifecycleListener.currentActivity.getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
          ActivityLifecycleListener.currentActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (!isFinish) {
                currentStatusBarColor = window.getStatusBarColor();
                currentNavigationBarColor = window.getNavigationBarColor();
                window.setStatusBarColor(Color.parseColor(notchColor));
                window.setNavigationBarColor(Color.parseColor(notchColor));
              } else {
                window.setStatusBarColor(currentStatusBarColor);
                window.setNavigationBarColor(currentNavigationBarColor);
              }
            }
          });
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Notch Color Exception: " + e.getMessage());
    }
  }
}
