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

import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
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
import androidx.recyclerview.widget.RecyclerView;
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
import com.cleverpush.util.SharedPreferencesManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.InputStream;
import java.lang.reflect.Field;
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
  private Activity activity;
  private final Banner data;
  private PopupWindow popup;
  private View popupRoot;
  private ViewPager2 viewPager2;
  public LinearLayout body;
  public FrameLayout frameLayout;
  public  ConstraintLayout parent;
  private ImageView bannerBackGroundImage;
  private int currentStatusBarColor;
  private int currentNavigationBarColor;
  private boolean isNotchColorChange = false;

  private AppBannerOpenedListener openedListener;
  int currentDisplayedPagePosition;

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
    if (ActivityLifecycleListener.currentActivity != null) {
      activity = ActivityLifecycleListener.currentActivity;
    }
    return canShow();
  }

  private View getRoot() {
    return activity.getWindow().getDecorView().getRootView();
  }

  private boolean canShow() {
    try {
      if (activity == null) {
        return false;
      }
      if (activity.isFinishing()) {
        return false;
      }
      if (activity.isDestroyed()) {
        return false;
      }
      Window window = activity.getWindow();
      if (window == null) {
        return false;
      }
      View decor = window.getDecorView();
      if (decor == null) {
        return false;
      }
      if (!decor.isShown()) {
        return false;
      }
      if (!decor.isAttachedToWindow()) {
        return false;
      }
      return true;
    } catch (Throwable t) {
      Logger.e(TAG, "Cannot show banner: Activity or window is not in a valid state (null, finishing, destroyed, or not attached to window)", t);
      return false;
    }
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

    isRootReady();

    int layoutId = R.layout.app_banner;

    popupRoot = createLayout(layoutId);
    if (popupRoot == null) {
      return;
    }
    parent = popupRoot.findViewById(R.id.parent);
    frameLayout = popupRoot.findViewById(R.id.frameLayout);
    body = popupRoot.findViewById(R.id.bannerBody);
    bannerBackGroundImage = popupRoot.findViewById(R.id.bannerBackgroundImage);

    popup = new PopupWindow(
        popupRoot,
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.MATCH_PARENT,
        true
    );
    currentDisplayedPagePosition = -1;

    if (isHTMLBanner()) {
      parent.setBackgroundColor(Color.TRANSPARENT);
      frameLayout.setBackgroundColor(Color.TRANSPARENT);
      body.setBackgroundColor(Color.TRANSPARENT);
      bannerBackGroundImage.setVisibility(View.GONE);
    } else {
      if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
        parent.setAlpha(0.0f);
      }

      bannerBackGroundImage.setVisibility(View.VISIBLE);
      composeBackground(bannerBackGroundImage, body, data.getBackground());
    }
    
    popup.setAnimationStyle(R.style.banner_animation);
    popup.setOnDismissListener(() -> {
      toggleShowing(false);
    });

    isInitialized = true;
  }

  private void toggleShowing(boolean isShowing) {
    SharedPreferences sharedPreferences = SharedPreferencesManager.getSharedPreferences(CleverPush.context);
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putBoolean(CleverPushPreferences.APP_BANNER_SHOWING, isShowing);
    editor.apply();
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
      if (CleverPush.getInstance(CleverPush.context).getAppBannerModule().getAppBannerClosedListener() != null) {
        CleverPush.getInstance(CleverPush.context).getAppBannerModule().getAppBannerClosedListener().closed();
      }
      this.toggleShowing(false);
      CleverPush.getInstance(CleverPush.context).getAppBannerModule().onAppBannerDismiss();
      CleverPush.getInstance(CleverPush.context).getAppBannerModule().clearBannerTrackList();
    }, 200);

  }

  public void moveToNextScreen() {
    int currentPosition = viewPager2.getCurrentItem();
    if (currentPosition < data.getScreens().size() - 1) {
      viewPager2.setCurrentItem(currentPosition + 1);
    }
  }

  public void moveToPreviousScreen() {
    int currentPosition = viewPager2.getCurrentItem();
    if (currentPosition > 0) {
      viewPager2.setCurrentItem(currentPosition - 1);
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
      return layout;
    } catch (InflateException inflateException) {
      try {
        int themeId = R.style.cleverpush_app_banner_theme;
        ContextThemeWrapper contextThemeWrapper = new ContextThemeWrapper(activity, themeId);

        LayoutInflater inflater = LayoutInflater.from(contextThemeWrapper);
        layout = inflater.inflate(layoutId, null);
        return layout;
      } catch (Exception exception) {
        Logger.e(TAG, "Error in App banner createLayout with theme cleverpush_app_banner_theme.", exception);
      }
    } catch (Exception exception) {
      Logger.e(TAG, "Error in App banner createLayout.", exception);
    }
    return null;
  }

  private void composeBackground(ImageView bannerBackground, LinearLayout body, BannerBackground background) {
    BannerBackground bg = background;
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

    if (bg != null) {
      processBackground(bannerBackground, body, bg);
    } else {
      GradientDrawable drawableBG = new GradientDrawable();
      drawableBG.setShape(GradientDrawable.RECTANGLE);
      if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
        drawableBG.setCornerRadius(10 * getPXScale());
      }
      if (isHTMLBanner()) {
        drawableBG.setColor(Color.TRANSPARENT);
      }
      drawableBG.setColor(Color.WHITE);
      bannerBackground.setImageBitmap(null);
      bannerBackground.setBackground(drawableBG);
    }
  }

  private void processBackground(ImageView bannerBackground, LinearLayout body, BannerBackground background) {
    if (background.getImageUrl() == null || background.getImageUrl().equalsIgnoreCase("null") || background.getImageUrl()
            .equalsIgnoreCase("")) {
      GradientDrawable drawableBG = new GradientDrawable();
      drawableBG.setShape(GradientDrawable.RECTANGLE);
      if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
        drawableBG.setCornerRadius(10 * getPXScale());
      }

      if (data.isDarkModeEnabled(activity) && background.getDarkColor() != null && !background.getDarkColor().isEmpty()) {
        drawableBG.setColor(ColorUtils.parseColor(background.getDarkColor()));
      } else if (background.getColor() != null && !background.getColor().isEmpty()) {
        drawableBG.setColor(ColorUtils.parseColor(background.getColor()));
      } else {
        drawableBG.setColor(Color.WHITE);
      }
      if (isHTMLBanner()) {
        drawableBG.setColor(Color.TRANSPARENT);
      }
      bannerBackground.setImageBitmap(null);
      bannerBackground.setBackground(drawableBG);
    } else if (background.getImageUrl() != null) {
      new Thread(() -> {
        try {
          String imageUrl;
          if (data.isDarkModeEnabled(activity) && background.getDarkImageUrl() != null) {
            imageUrl = background.getDarkImageUrl();
          } else {
            imageUrl = background.getImageUrl();
          }

          InputStream in = new URL(imageUrl).openStream();
          Bitmap bitmap = BitmapFactory.decodeStream(in);
          if (bitmap != null) {
            bannerBackground.setImageBitmap(bitmap);
          }
        } catch (Exception ex) {
          Logger.e(TAG, "Error at setting background image in app banner.", ex);
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
      runInMain(() -> {
        if (!canShow()) {
          return;
        }
        popup.showAtLocation(getRoot(), Gravity.TOP, 0, 0);
        toggleShowing(true);
      });
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

          runInMain(() -> {
            if (!canShow()) {
              return;
            }
            popup.showAtLocation(getRoot(), Gravity.TOP, 0, 0);
            toggleShowing(true);
          });
          break;
        case POSITION_TYPE_BOTTOM:
          mConstraintSet.clone(mConstraintLayout);
          mConstraintSet.clear(R.id.frameLayout, ConstraintSet.TOP);
          mConstraintSet.clear(R.id.frameLayout, ConstraintSet.BOTTOM);
          mConstraintSet.connect(R.id.frameLayout, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
              40);
          mConstraintSet.applyTo(mConstraintLayout);

          runInMain(() -> {
            if (!canShow()) {
              return;
            }
            popup.showAtLocation(getRoot(), Gravity.BOTTOM, 0, 0);
            toggleShowing(true);
          });
          break;
        default:
          popup.setFocusable(true);
          popup.setOutsideTouchable(false);
          popup.setTouchable(true);
          popup.setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);
          popup.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

          popup.setTouchInterceptor((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
              v.performClick();
            }
            return false;  // Ensures touch events propagate to child views
          });
          runInMain(() -> {
            if (!canShow()) {
              return;
            }
            popup.showAtLocation(getRoot(), Gravity.CENTER, 0, 0);
            toggleShowing(true);
          });
          break;
      }
    }
  }

  private void setUpBannerBlocks() {
    viewPager2 = popupRoot.findViewById(R.id.carousel_pager);
    TabLayout tabLayout = popupRoot.findViewById(R.id.carousel_pager_tab_layout);

    if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        viewPager2.setNestedScrollingEnabled(true);
      }
      viewPager2.getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
      viewPager2.setUserInputEnabled(true);
      viewPager2.requestDisallowInterceptTouchEvent(false);
    }

    if (data.isCloseButtonEnabled()) {
      ImageButton buttonClose;
      if (data.isCloseButtonPositionStaticEnabled()) {
        buttonClose = popupRoot.findViewById(R.id.buttonClose);
        if (isHTMLBanner()) {
          ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) buttonClose.getLayoutParams();
          int marginInDp = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, activity.getResources().getDisplayMetrics());
          params.setMargins(marginInDp, marginInDp, marginInDp, marginInDp);
          buttonClose.setLayoutParams(params);
        }
      } else {
        buttonClose = popupRoot.findViewById(R.id.buttonCloseOverlap);
      }
      buttonClose.setVisibility(View.VISIBLE);
      buttonClose.setOnClickListener(view -> dismiss());
    }

    viewPager2.setUserInputEnabled(true);

    if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
      try {
        Field recyclerViewField = ViewPager2.class.getDeclaredField("mRecyclerView");
        recyclerViewField.setAccessible(true);
        RecyclerView recyclerView = (RecyclerView) recyclerViewField.get(viewPager2);
        Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
        touchSlopField.setAccessible(true);
        int touchSlop = (int) touchSlopField.get(recyclerView);
        touchSlopField.set(recyclerView, touchSlop * 2);
      } catch (Exception e) {
        Logger.e(TAG, "Error adjusting ViewPager2 touch sensitivity", e);
      }
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

    if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
      viewPager2.post(() -> {
        try {
          RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
          RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
          if (layoutManager != null) {
            View currentView = layoutManager.findViewByPosition(viewPager2.getCurrentItem());
            if (currentView != null) {
              updatePagerHeightForChild(currentView, viewPager2);
            }
          }
          recyclerView.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
              try {
                int pos = recyclerView.getChildAdapterPosition(view);
                if (pos == viewPager2.getCurrentItem()) {
                  updatePagerHeightForChild(view, viewPager2);
                  recyclerView.removeOnChildAttachStateChangeListener(this);
                }
              } catch (Exception ignore) { }
            }
            @Override
            public void onChildViewDetachedFromWindow(View view) { }
          });
        } catch (Exception e) {
          Logger.e(TAG, "Error setting initial ViewPager2 height", e);
        }
      });
    }

    viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
      @Override
      public void onPageSelected(int position) {
        super.onPageSelected(position);

        if (data.getEnableMultipleScreens()) {
          composeBackground(bannerBackGroundImage, body, data.getScreens().get(position).getBackground());
        }

        if (currentDisplayedPagePosition != position) {
          currentDisplayedPagePosition = position;

          if (data.getEnableMultipleScreens() && data.getScreens().size() > position) {
            String screenId = data.getScreens().get(position).getId();
            boolean isScreenAlreadyShown = CleverPush.getInstance(CleverPush.context).getAppBannerModule().isBannerScreenDelivered(screenId);
            if (!isScreenAlreadyShown) {
              CleverPush.getInstance(CleverPush.context).getAppBannerModule().setIsBannerScreenDelivered(screenId);
            }
            CleverPush.getInstance(CleverPush.context).getAppBannerModule().sendBannerEvent("delivered", data, null, screenId, false, isScreenAlreadyShown);
          } else {
            CleverPush.getInstance(CleverPush.context).getAppBannerModule().sendBannerEvent("delivered", data);
          }
        }

        if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
          try {
            RecyclerView recyclerView = (RecyclerView) viewPager2.getChildAt(0);
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager != null) {
              View currentView = layoutManager.findViewByPosition(position);
              if (currentView != null) {
                updatePagerHeightForChild(currentView, viewPager2);
              }
            }
          } catch (Exception e) {
            Logger.e(TAG, "Error updating ViewPager2 height on page selected", e);
          }
        }
      }
    });

    viewPager2.setPageTransformer((page, position) -> {
      if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
        updatePagerHeightForChild(page, viewPager2);
      }
    });

  }

  public void updatePagerHeightForChild(View view, ViewPager2 viewPager2) {
    view.post(() -> {
      try {
        View scrollView = view.findViewById(R.id.scrollView);
        View content = view.findViewById(R.id.carouselBannerBody);

        int targetWidth = view.getWidth() > 0 ? view.getWidth() : activity.getResources().getDisplayMetrics().widthPixels;
        int wMeasureSpec = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY);
        int hMeasureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);

        int measuredContentHeight;
        if (content != null) {
          content.measure(wMeasureSpec, hMeasureSpec);
          measuredContentHeight = content.getMeasuredHeight();
          if (scrollView != null) {
            measuredContentHeight += scrollView.getPaddingTop() + scrollView.getPaddingBottom();
          }
        } else {
          view.measure(wMeasureSpec, hMeasureSpec);
          measuredContentHeight = view.getMeasuredHeight();
        }

        int maxHeight = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.77);
        int desiredHeight = Math.min(measuredContentHeight, maxHeight);
        if (desiredHeight <= 0) {
          desiredHeight = maxHeight;
        }

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, desiredHeight);
        viewPager2.setLayoutParams(layoutParams);
        viewPager2.invalidate();
      } catch (Exception ignore) { }
    });
  }

  public class tryShowSafe extends AsyncTask<String, Void, Boolean> {
    @Override
    protected Boolean doInBackground(String... strings) {
      if (isRootReady()) {
        return true;
      }
      // Only reschedule if the activity is still valid (not finishing/destroyed)
      if (activity != null
          && !activity.isFinishing()
          && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed())) {
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
          Logger.e(TAG, "Error in displaying banner.", e);
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
              window.getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
                int statusBarInsetTop = 0;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                  statusBarInsetTop = insets.getInsets(WindowInsets.Type.statusBars()).top;
                }
                view.setBackgroundColor(Color.parseColor(notchColor));

                // Adjust padding to avoid overlap
                view.setPadding(0, statusBarInsetTop, 0, 0);
                return insets;
              });
            }
          });
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error in setting notch color for HTML banner", e);
    }
  }
}
