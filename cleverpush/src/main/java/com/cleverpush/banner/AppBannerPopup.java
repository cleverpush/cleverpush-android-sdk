package com.cleverpush.banner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
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

import com.cleverpush.CleverPush;
import com.cleverpush.CleverPushPreferences;
import com.cleverpush.R;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerScreens;
import com.cleverpush.banner.models.blocks.Alignment;
import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AppBannerPopup {

    private static final String POSITION_TYPE_TOP = "top";
    private static final String POSITION_TYPE_BOTTOM = "bottom";
    private static final String POSITION_TYPE_FULL = "full";
    private static final String TAG = "CleverPush/AppBanner";

    private static SpringForce getDefaultForce(float finalValue) {
        SpringForce force = new SpringForce(finalValue);
        force.setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
        float DEFAULT_STIFFNESS_RATIO = 300f;
        force.setStiffness(DEFAULT_STIFFNESS_RATIO);

        return force;
    }

    private static final Map<Alignment, Integer> alignmentMap = new HashMap<>();

    static {
        alignmentMap.put(Alignment.Left, View.TEXT_ALIGNMENT_TEXT_START);
        alignmentMap.put(Alignment.Center, View.TEXT_ALIGNMENT_CENTER);
        alignmentMap.put(Alignment.Right, View.TEXT_ALIGNMENT_TEXT_END);
    }

    private final Handler mainHandler;

    private final Activity activity;
    private final Banner data;

    private PopupWindow popup;
    private View popupRoot;
    private ViewPager2 viewPager2;
    private LinearLayout body;

    private AppBannerOpenedListener openedListener;

    private boolean isInitialized = false;

    public void setOpenedListener(AppBannerOpenedListener openedListener) {
        this.openedListener = openedListener;
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

    AppBannerPopup(Activity activity, Banner data) {
        this.activity = activity;
        this.data = data;

        mainHandler = new Handler(activity.getMainLooper());
    }

    public void init() {
        if (isInitialized) {
            return;
        }

        int layoutId = R.layout.app_banner;

        popupRoot = createLayout(layoutId);
        body = popupRoot.findViewById(R.id.bannerBody);
        ImageView bannerBackGroundImage = popupRoot.findViewById(R.id.bannerBackgroundImage);

        popup = new PopupWindow(
                popupRoot,
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                true
        );

        composeBackground(bannerBackGroundImage, body);
        popup.setAnimationStyle(R.style.banner_animation);

        popup.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override
            public void onDismiss() {
                toggleShowing(false);
            }
        });

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
            Log.e(TAG, "Must be initialized");
            return;
        }

        runInMain(() -> animateBody(0f, getRoot().getHeight()));
        runInMain(() -> {
            popup.dismiss();
            this.toggleShowing(false);
        }, 200);

    }

    public void moveToNextScreen() {
        int currentPosition = viewPager2.getCurrentItem();
        if (currentPosition < data.getScreens().size() - 1) {
            viewPager2.setCurrentItem(currentPosition + 1);
        }
    }

    public AppBannerOpenedListener getOpenedListener() {
        return openedListener;
    }

    private View createLayout(int layoutId) {
        View layout = activity.getLayoutInflater().inflate(layoutId, null);
        layout.setOnClickListener(view -> dismiss());

        return layout;
    }

    private void composeBackground(ImageView bannerBackground, LinearLayout body) {
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
            if (!data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
                drawableBG.setCornerRadius(10 * getPXScale());
            }
            if (bg.getColor() != null) {
                drawableBG.setColor(this.parseColor(bg.getColor()));
            } else {
                drawableBG.setColor(Color.WHITE);
            }
            bannerBackground.setBackground(drawableBG);
        } else if (bg.getImageUrl() != null) {
            new Thread(() -> {
                try {
                    InputStream in = new URL(bg.getImageUrl()).openStream();
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    if (bitmap != null) {
                        bannerBackground.setImageBitmap(bitmap);
                    }
                } catch (Exception ignored) {
                    Log.e(TAG, ignored.getLocalizedMessage());
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
        mainHandler.post(runnable);
    }

    private void runInMain(Runnable runnable, long delay) {
        if (delay <= 0L) {
            mainHandler.post(runnable);
        } else {
            mainHandler.postDelayed(runnable, delay);
        }
    }

    private int parseColor(String colorStr) {
        if (colorStr.charAt(0) == '#' && colorStr.length() == 4) {
            colorStr = "#" + colorStr.charAt(1) + colorStr.charAt(1) + colorStr.charAt(2) + colorStr.charAt(2) + colorStr.charAt(3) + colorStr.charAt(3);
        }
        int color = Color.BLACK;
        try {
            color = Color.parseColor(colorStr);
        } catch (Exception ex) {

        }
        return color;
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
                displayBanner(body);
                animateBody(getRoot().getHeight(), 0f);
            }
        }
    }

    private void displayBanner(LinearLayout body) {
        if (!data.isCarouselEnabled()) {
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

            body.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            runInMain(() -> popup.showAtLocation(popupRoot, Gravity.TOP, 0, 0));
        } else {
            body.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

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
                    mConstraintSet.connect(R.id.frameLayout, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 40);
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
        ImageButton buttonClose = popupRoot.findViewById(R.id.buttonClose);

        if (data.isCloseButtonEnabled()) {
            buttonClose.setVisibility(View.VISIBLE);
            buttonClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismiss();
                }
            });
        }

        if (data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
            viewPager2.setLayoutParams(layoutParams);
        }

        AppBannerCarouselAdapter appBannerCarouselAdapter = new AppBannerCarouselAdapter(activity, data, this, openedListener);
        viewPager2.setAdapter(appBannerCarouselAdapter);
        if (data.getScreens().size() > 1) {
            tabLayout.setVisibility(View.VISIBLE);
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
            if (popupRoot.getMeasuredHeight() > view.getMeasuredHeight()) {
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, view.getMeasuredHeight());
            } else {
                int height = (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.77);
                layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
            }

            viewPager2.setLayoutParams(layoutParams);
            viewPager2.invalidate();
        });
    }
}
