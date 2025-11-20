package com.cleverpush.banner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.Spanned;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.text.HtmlCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cleverpush.CleverPush;
import com.cleverpush.R;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.BannerAction;
import com.cleverpush.banner.models.BannerScreens;
import com.cleverpush.banner.models.blocks.Alignment;
import com.cleverpush.banner.models.blocks.BannerBlock;
import com.cleverpush.banner.models.blocks.BannerButtonBlock;
import com.cleverpush.banner.models.blocks.BannerHTMLBlock;
import com.cleverpush.banner.models.blocks.BannerImageBlock;
import com.cleverpush.banner.models.blocks.BannerTextBlock;
import com.cleverpush.listener.AppBannerOpenedListener;
import com.cleverpush.util.ColorUtils;
import com.cleverpush.util.FontUtils;
import com.cleverpush.util.Logger;
import com.cleverpush.util.VoucherCodeUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Locale;

public class AppBannerCarouselAdapter extends RecyclerView.Adapter<AppBannerCarouselAdapter.ViewHolder> {

  private static final String TAG = "CleverPush/AppBanner";
  private static final String CONTENT_TYPE_HTML = "html";
  private static final String POSITION_TYPE_FULL = "full";
  private static final Map<Alignment, Integer> alignmentMap = new HashMap<>();
  private static final Map<Alignment, Integer> gravityMap = new HashMap<>();
  private final Activity activity;
  private final Banner data;
  private final List<BannerScreens> screens = new LinkedList<>();
  private final AppBannerPopup appBannerPopup;
  private String voucherCode;

  static {
    alignmentMap.put(Alignment.Left, View.TEXT_ALIGNMENT_TEXT_START);
    alignmentMap.put(Alignment.Center, View.TEXT_ALIGNMENT_CENTER);
    alignmentMap.put(Alignment.Right, View.TEXT_ALIGNMENT_TEXT_END);
  }

  static {
    gravityMap.put(Alignment.Left, Gravity.START);
    gravityMap.put(Alignment.Center, Gravity.CENTER);
    gravityMap.put(Alignment.Right, Gravity.END);
  }

  AppBannerCarouselAdapter(Activity activity, Banner banner, AppBannerPopup appBannerPopup,
                           AppBannerOpenedListener openedListener) {
    this.activity = activity;
    this.data = banner;
    this.appBannerPopup = appBannerPopup;
    screens.addAll(data.getScreens());
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(activity).inflate(R.layout.app_banner_carousel_item, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    LinearLayout body = holder.itemView.findViewById(R.id.carouselBannerBody);
    try {
      body.removeAllViews();
      try {
        if (isBannerPositionFull() && data.getContentType() != null && !data.getContentType().equalsIgnoreCase(CONTENT_TYPE_HTML)) {
          body.setGravity(Gravity.CENTER);
        }
      } catch (Exception e) {
        Logger.e(TAG, "Error in AppBanner displaying component to center when position is full.", e);
      }

      HashMap<String, String> currentVoucherCodePlaceholder = CleverPush.getInstance(CleverPush.context).getAppBannerModule().getCurrentVoucherCodePlaceholder();
      if (currentVoucherCodePlaceholder != null && currentVoucherCodePlaceholder.containsKey(data.getId())) {
        voucherCode = currentVoucherCodePlaceholder.get(data.getId());
      }

      if (data.getContentType() != null && data.getContentType().equalsIgnoreCase(CONTENT_TYPE_HTML)) {
        composeHtmlBanner(body, data.getContent());
      } else {
        List<BannerBlock> bannerBlocks = data.getScreens().get(position).getBlocks();
        final int[] pendingBlocks = {bannerBlocks.size()}; // Track pending blocks

        for (BannerBlock bannerBlock : bannerBlocks) {
          activity.runOnUiThread(() -> {
            try {
              switch (bannerBlock.getType()) {
                case Text:
                  composeTextBlock(body, (BannerTextBlock) bannerBlock, position);
                  break;
                case Image:
                  composeImageBlock(body, (BannerImageBlock) bannerBlock, position);
                  break;
                case Button:
                  composeButtonBlock(body, (BannerButtonBlock) bannerBlock, position);
                  break;
                case HTML:
                  composeHtmlBLock(body, (BannerHTMLBlock) bannerBlock);
                  break;
              }
            } finally {
              synchronized (pendingBlocks) {
                pendingBlocks[0]--;
                if (pendingBlocks[0] == 0) {
                  activity.runOnUiThread(this::onBlocksComposed);
                }
              }
            }
          });

          body.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(false);
            return false;
          });
        }
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner onBindViewHolder.", e);
    }
  }

  private void onBlocksComposed() {
    try {
      new Handler(Looper.getMainLooper()).postDelayed(() -> {
        if (!isBannerPositionFull() && !appBannerPopup.isHTMLBanner()) {
          appBannerPopup.getViewPager2().setPageTransformer((page, position) -> {
            appBannerPopup.updatePagerHeightForChild(page, appBannerPopup.getViewPager2());

            appBannerPopup.parent.animate().alpha(1.0f).setDuration(500).start();
          });
        }
      }, 1000);
    } catch (Exception e) {
      Logger.e(TAG, "Error while updating child height in AppBannerCarouselAdapter onBlocksComposed. " + e.getLocalizedMessage(), e);
    }
  }

  @Override
  public int getItemCount() {
    return screens.size();
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public ViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }

  private boolean isOpenUrlAction(BannerAction action) {
    return action.getType() != null && action.getType().equalsIgnoreCase("url");
  }

  private void onClickListener(BannerAction action, List<BannerAction> actions) {
    try {
      if (actions != null && actions.size() > 0) {
        List<BannerAction> sortedActions = new ArrayList<>(actions);
        Collections.sort(sortedActions, new Comparator<BannerAction>() {
          @Override
          public int compare(BannerAction a, BannerAction b) {
            boolean aIsOpenUrl = isOpenUrlAction(a);
            boolean bIsOpenUrl = isOpenUrlAction(b);

            if (aIsOpenUrl && !bIsOpenUrl) {
              return 1;
            } else if (!aIsOpenUrl && bIsOpenUrl) {
              return -1;
            }
            return 0;
          }
        });

        for (int i = 0; i < actions.size(); i++) {
          boolean isLastElement = (i == actions.size() - 1);
          actions.get(i).setBlockId(action.getBlockId());
          actions.get(i).setMultipleScreenId(action.getMultipleScreenId());
          onClickListener(actions.get(i), isLastElement);
        }
      } else {
        onClickListener(action, true);
      }

      String blockId, screenId;
      blockId = action.getBlockId();
      screenId = action.getMultipleScreenId();

      boolean isElementAlreadyClicked = CleverPush.getInstance(CleverPush.context).getAppBannerModule().isBannerElementClicked(blockId);
      if (!isElementAlreadyClicked) {
        CleverPush.getInstance(CleverPush.context).getAppBannerModule().setIsBannerElementClicked(blockId);
      }

      CleverPush.getInstance(CleverPush.context).getAppBannerModule().sendBannerEvent("clicked", data, blockId, screenId, isElementAlreadyClicked, false);

    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner onClickListener", e);
    }
  }

  private void onClickListener(BannerAction action,  boolean isLastActionElement) {
    try {
      if (action.isOpenInWebView() && action.getUrl() != null && !action.getUrl().isEmpty()) {
        String URL = VoucherCodeUtils.replaceVoucherCodeString(action.getUrl(), voucherCode);
        WebViewActivity.launch(activity, URL);
      } else if (action.getScreen() != null && !action.getScreen().isEmpty()) {
        for (int i = 0; i < screens.size(); i++) {
          if (screens.get(i).getId() != null && screens.get(i).getId().equals(action.getScreen())) {
            appBannerPopup.moveToNextScreen(i);
            break;
          }
        }
      } else {
        if (isLastActionElement) {
          appBannerPopup.moveToNextScreen();
        }
      }

      if (action.getDismiss()) {
        appBannerPopup.dismiss();
      }

      if (action.isOpenBySystem() && action.getUrl() != null && !action.getUrl().isEmpty()) {
        String URL = VoucherCodeUtils.replaceVoucherCodeString(action.getUrl(), voucherCode);
        activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(URL)));
      }

      if (appBannerPopup.getOpenedListener() != null) {
        appBannerPopup.getOpenedListener().opened(action);
      }
    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner onClickListener", e);
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private void composeButtonBlock(LinearLayout body, BannerButtonBlock block, int position) {
    @SuppressLint("InflateParams") Button button =
        (Button) activity.getLayoutInflater().inflate(R.layout.app_banner_button, null);
    String text = VoucherCodeUtils.replaceVoucherCodeString(block.getText(), voucherCode);
    button.setText(text);
    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4 / 3);

    if (block.getFamily() != null) {
      try {
        Typeface font = FontUtils.findFont(activity, block.getFamily());
        button.setTypeface(font);
      } catch (Exception ex) {
        Logger.e(TAG, "Error in AppBanner composeButtonBlock setTypeface.", ex);
      }
    }

    String textColor;
    if (appBannerPopup.getData().isDarkModeEnabled(activity) && block.getDarkColor() != null) {
      textColor = block.getDarkColor();
    } else {
      textColor = block.getColor();
    }
    button.setTextColor(ColorUtils.parseColor(textColor));

    Integer alignment = alignmentMap.get(block.getAlignment());
    button.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

    Integer gravity = gravityMap.get(block.getAlignment());
    button.setGravity(gravity == null ? Gravity.CENTER : gravity);

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );

    // Determine if the device is a tablet
    boolean isTablet = (activity.getResources().getConfiguration().screenLayout
        & Configuration.SCREENLAYOUT_SIZE_MASK)
        >= Configuration.SCREENLAYOUT_SIZE_LARGE;

    // Set margins based on device type
    if (isTablet) {
      params.setMargins(0, 10, 0, 10);
    } else {
      params.setMargins(0, 20, 0, 20);
    }
    button.setLayoutParams(params);

    GradientDrawable bg = new GradientDrawable();
    bg.setShape(GradientDrawable.RECTANGLE);
    bg.setCornerRadius(block.getRadius() * getPXScale());

    String backgroundColor;
    if (appBannerPopup.getData().isDarkModeEnabled(activity) && block.getDarkBackground() != null) {
      backgroundColor = block.getDarkBackground();
    } else {
      backgroundColor = block.getBackground();
    }
    bg.setColor(ColorUtils.parseColor(backgroundColor));
    button.setBackground(bg);

    if (block.getAction() != null) {
      BannerAction action = block.getAction();
      List<BannerAction> actions = block.getActions();
      action.setBlockId(block.getId());
      action.setMultipleScreenId(data.getScreens().get(position).getId());
      button.setOnClickListener(view -> this.onClickListener(action, actions));
    }

    button.setOnTouchListener((view, motionEvent) -> {
      int action = motionEvent.getAction();
      if (action == MotionEvent.ACTION_DOWN) {
        view.animate().cancel();
        view.animate().scaleX(0.98f).setDuration(200).start();
        view.animate().scaleY(0.98f).setDuration(200).start();
        return false;
      } else if (action == MotionEvent.ACTION_UP) {
        view.animate().cancel();
        view.animate().scaleX(1f).setDuration(200).start();
        view.animate().scaleY(1f).setDuration(200).start();
        return false;
      }

      return false;
    });

    body.addView(button);
  }

  private void composeTextBlock(LinearLayout body, BannerTextBlock block, int position) {
    @SuppressLint("InflateParams") TextView textView =
        (TextView) activity.getLayoutInflater().inflate(R.layout.app_banner_text, null);

    if (block.getHtml() != null && !block.getHtml().isEmpty()) {
      try {
        String replacedHtml = VoucherCodeUtils.replaceVoucherCodeString(block.getHtml(), voucherCode);
        Spanned formattedText = HtmlCompat.fromHtml(replacedHtml, HtmlCompat.FROM_HTML_MODE_LEGACY);
        textView.setText(formattedText);
      } catch (Exception e) {
        Logger.e(TAG, "Error parsing HTML in composeTextBlock for banner text block", e);
        String fallback = VoucherCodeUtils.replaceVoucherCodeString(block.getText(), voucherCode);
        textView.setText(fallback);
      }
    } else {
      String text = VoucherCodeUtils.replaceVoucherCodeString(block.getText(), voucherCode);
      textView.setText(text);
    }
    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4 / 3);

    String textColor;
    if (appBannerPopup.getData().isDarkModeEnabled(activity) && block.getDarkColor() != null) {
      textColor = block.getDarkColor();
    } else {
      textColor = block.getColor();
    }
    textView.setTextColor(ColorUtils.parseColor(textColor));

    if (block.getFamily() != null) {
      try {
        Typeface font = FontUtils.findFont(activity, block.getFamily());
        textView.setTypeface(font);
      } catch (Exception ex) {
        Logger.e(TAG, "Error in AppBanner composeTextBlock setTypeface.", ex);
      }
    }

    Integer alignment = alignmentMap.get(block.getAlignment());
    textView.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

    Integer gravity = gravityMap.get(block.getAlignment());
    textView.setGravity(gravity == null ? Gravity.CENTER : gravity);

    body.addView(textView);
  }

  private void composeImageBlock(LinearLayout body, BannerImageBlock block, int blockPosition) {
    @SuppressLint("InflateParams") ConstraintLayout imageLayout =
            (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_image, null);
    ImageView img = imageLayout.findViewById(R.id.imageView);
    ProgressBar progressBar = imageLayout.findViewById(R.id.progressBar);
    progressBar.setVisibility(View.VISIBLE);

    float imageWidth = block.getImageWidth();
    float imageHeight = block.getImageHeight();

    // Check if device is a tablet in landscape mode
    boolean isTablet = (activity.getResources().getConfiguration().screenLayout &
            Configuration.SCREENLAYOUT_SIZE_MASK) >=
            Configuration.SCREENLAYOUT_SIZE_LARGE;
    boolean isLandscape = activity.getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;

    ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) img.getLayoutParams();

    img.setLayoutParams(layoutParams);

    ConstraintSet imgConstraints = new ConstraintSet();
    imgConstraints.clone(imageLayout);

    float widthPercentage;
    if (isTablet && isLandscape) {
      widthPercentage = 0.6f;
      imgConstraints.constrainPercentWidth(img.getId(), widthPercentage);
      float aspectRatio = imageWidth / imageHeight;
      imgConstraints.setDimensionRatio(img.getId(), aspectRatio + ":1");
    } else {
      widthPercentage = Math.min(100, Math.max(0, block.getScale())) / 100.0f;
      imgConstraints.constrainPercentWidth(img.getId(), widthPercentage);
      float aspectRatio = 1.0f;
      float height = widthPercentage / aspectRatio * 100;
      imgConstraints.constrainPercentHeight(img.getId(), height);
    }

    imgConstraints.applyTo(imageLayout);

    new Thread(() -> {
      HttpURLConnection connection = null;
      InputStream in = null;
      try {
        String imageUrl;
        if (appBannerPopup.getData().isDarkModeEnabled(activity)
                && block.getDarkImageUrl() != null && !block.getDarkImageUrl().isEmpty()) {
          imageUrl = block.getDarkImageUrl();
        } else {
          imageUrl = block.getImageUrl();
        }

        URL url = new URL(imageUrl);
        connection = (HttpURLConnection) url.openConnection();
        connection.setDoInput(true);

        // Get the content type from the response headers
        String contentType = connection.getHeaderField("Content-Type");

        // Checks whether the content at the specified URL is a GIF image.
        boolean isGif = contentType != null && contentType.toLowerCase().startsWith("image/gif");

        in = connection.getInputStream();
        final Bitmap bitmap = BitmapFactory.decodeStream(in);

        activity.runOnUiThread(() -> {
          if (isGif) {
            Glide.with(CleverPush.context)
                .asGif()
                .load(imageUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(img);
          } else {
            if (bitmap != null) {
              img.setImageBitmap(bitmap);
            }
          }
          progressBar.setVisibility(View.GONE);

          try {
            new Handler().postDelayed(new Runnable() {
              @Override
              public void run() {
                if (!isBannerPositionFull()) {
                  appBannerPopup.getViewPager2().setPageTransformer((page, position1) -> {
                    appBannerPopup.updatePagerHeightForChild(page, appBannerPopup.getViewPager2());
                  });
                }
              }
            }, 1000);
          } catch (Exception e) {
            Logger.e(TAG, "Error while updating child height in composeImageBlock. " + e.getLocalizedMessage(), e);
          }
        });

      } catch (Exception e) {
        Logger.e(TAG, "Error in AppBanner composeImageBlock.", e);
        if (activity != null) {
          activity.runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
          });
        }
      } finally {
        if (connection != null) {
          connection.disconnect();
        }
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            Logger.e(TAG, "Error in closing InputStream in AppBanner composeImageBlock.", e);
          }
        }
      }
    }).start();

    body.addView(imageLayout);

    if (block.getAction() != null) {
      BannerAction action = block.getAction();
      List<BannerAction> actions = block.getActions();
      action.setBlockId(block.getId());
      action.setMultipleScreenId(data.getScreens().get(blockPosition).getId());
      img.setOnClickListener(view -> this.onClickListener(action, actions));
    }

    imageLayout.setOnTouchListener(new View.OnTouchListener() {
      private float downX, downY;
      private final int touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
      private boolean isScrolling = false;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            downX = event.getX();
            downY = event.getY();
            isScrolling = false;
            v.getParent().requestDisallowInterceptTouchEvent(true);
            break;
          case MotionEvent.ACTION_MOVE:
            float deltaX = Math.abs(event.getX() - downX);
            float deltaY = Math.abs(event.getY() - downY);

            if (!isScrolling) {
              if (deltaY > touchSlop && deltaY > deltaX) {
                isScrolling = true; // Allow vertical scroll
                v.getParent().requestDisallowInterceptTouchEvent(false);
              } else if (deltaX > touchSlop) {
                v.getParent().requestDisallowInterceptTouchEvent(true); // Allow horizontal swipe
              }
            }
            break;
          case MotionEvent.ACTION_UP:
          case MotionEvent.ACTION_CANCEL:
            v.getParent().requestDisallowInterceptTouchEvent(false);
            isScrolling = false;
            break;
        }
        return false;
      }
    });

    img.setOnTouchListener(new View.OnTouchListener() {
      private float downX, downY;
      private final int touchSlop = ViewConfiguration.get(activity).getScaledTouchSlop();
      private boolean isScrolling = false;

      @Override
      public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
          case MotionEvent.ACTION_DOWN:
            downX = event.getX();
            downY = event.getY();
            isScrolling = false;
            v.getParent().requestDisallowInterceptTouchEvent(true);
            break;
          case MotionEvent.ACTION_MOVE:
            float deltaX = Math.abs(event.getX() - downX);
            float deltaY = Math.abs(event.getY() - downY);

            if (!isScrolling) {
              if (deltaY > touchSlop && deltaY > deltaX) {
                isScrolling = true; // Allow vertical scroll
                v.getParent().requestDisallowInterceptTouchEvent(false);
              } else if (deltaX > touchSlop) {
                v.getParent().requestDisallowInterceptTouchEvent(true); // Allow horizontal swipe
              }
            }
            break;
          case MotionEvent.ACTION_UP:
          case MotionEvent.ACTION_CANCEL:
            v.getParent().requestDisallowInterceptTouchEvent(false);
            isScrolling = false;
            break;
        }
        return false;
      }
    });
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void composeHtmlBLock(LinearLayout body, BannerHTMLBlock block) {
    @SuppressLint("InflateParams") LinearLayout webLayout =
        (LinearLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html_block, null);
    WebView webView = webLayout.findViewById(R.id.webView);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setVerticalScrollBarEnabled(false);
    webView.setHorizontalScrollBarEnabled(false);

    webView.setWebViewClient(new WebViewClient() {
      @Override
      public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        webView.evaluateJavascript(
            "CleverPush.trackEvent = function(eventId, properties) {\n" +
                "  CleverPush.trackEventStringified(eventId, properties ? JSON.stringify(properties) : null);\n" +
                "};\n" +
                "CleverPush.trackClick = function(buttonId, customData) {\n" +
                "  CleverPush.trackClickStringified(buttonId, customData ? JSON.stringify(customData) : null);\n" +
                "};\n",
            null
        );
      }
    });

    webView.loadUrl(block.getUrl());
    webView.addJavascriptInterface(new CleverpushInterface(), "CleverPush");

    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        pxToDp(Integer.parseInt(block.getHeight()))
    );
    params.setMargins(0, 0, 0, 20);
    webView.setLayoutParams(params);
    webView.setBackgroundColor(Color.TRANSPARENT);
    body.addView(webLayout);
  }

  private float getPXScale() {
    int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
    return Math.max(Math.min(screenWidth / 400.0f, 10f), 1.0f);
  }

  private void fixFullscreenHtmlBannerUI(LinearLayout body, ConstraintLayout webLayout, WebView webView) {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    activity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

    int screenWidth = displayMetrics.widthPixels;
    int screenHeight = displayMetrics.heightPixels;

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(webLayout);
    constraintSet.constrainMinHeight(R.id.webView, screenHeight);
    constraintSet.applyTo(webLayout);

    body.setPadding(0, 0, 0, 0);

    ViewGroup.LayoutParams layoutParams = webView.getLayoutParams();
    layoutParams.width = screenWidth;
    layoutParams.height = screenHeight;
    webView.setLayoutParams(layoutParams);

    appBannerPopup.getViewPager2().getChildAt(0).setOverScrollMode(View.OVER_SCROLL_NEVER);
  }

  /**
   * Will compose and add HTML Banner to the body of banner layout.
   *
   * @param body        parent layout to add HTML view
   * @param htmlContent html content which will be displayed in banner
   */
  @SuppressLint("SetJavaScriptEnabled")
  private void composeHtmlBanner(LinearLayout body, String htmlContent) {
    try {
      activity.runOnUiThread(() -> {
        String html = VoucherCodeUtils.replaceVoucherCodeString(htmlContent, voucherCode);
        String lower = html.toLowerCase(Locale.ROOT);
        String jsToInject = "" +
                "<script type=\"text/javascript\">\n" +
                "// Below conditions will take care of all ids and classes which contains defined keywords at start and end of string\n"
                +
                "var closeBtns = document.querySelectorAll('[id^=\"close\"], [id$=\"close\"], [class^=\"close\"], [class$=\"close\"]');\n"
                +
                "function onCloseClick() {\n" +
                "  try {\n" +
                "    CleverPush.closeBanner();\n" +
                "  } catch (error) {\n" +
                "    console.log('Caught error on closeBtn click', error);\n" +
                "  }\n" +
                "}\n" +
                "for (var i = 0; i < closeBtns.length; i++) {\n" +
                "  closeBtns[i].addEventListener('click', onCloseClick);\n" +
                "}\n" +
                "CleverPush.trackEvent = function(eventId, properties) {\n" +
                "  CleverPush.trackEventStringified(eventId, properties ? JSON.stringify(properties) : null);\n" +
                "};\n" +
                "CleverPush.trackClick = function(buttonId, customData) {\n" +
                "  CleverPush.trackClickStringified(buttonId, customData ? JSON.stringify(customData) : null);\n" +
                "};\n" +
                "</script>\n";
        String htmlWithJs;
        if (lower.contains("</body>")) {
          htmlWithJs = html.replaceAll("(?i)</body>", jsToInject + "</body>");
        } else if (lower.contains("<body")) {
          htmlWithJs = html + jsToInject;
        } else {
          htmlWithJs =
                  "<!DOCTYPE html><html><head>" +
                          "<meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                          "</head><body>" +
                          html +
                          jsToInject +
                          "</body></html>";
        }
        ConstraintLayout webLayout =
                (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html, null);
        WebView webView = webLayout.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.addJavascriptInterface(new CleverpushInterface(), "CleverPush");
        webView.setWebViewClient(new AppBannerWebViewClient());

        webView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
          ViewGroup.LayoutParams params = webView.getLayoutParams();
          params.width = ViewGroup.LayoutParams.MATCH_PARENT;
          params.height = ViewGroup.LayoutParams.MATCH_PARENT;
          webView.setLayoutParams(params);
          webView.requestLayout();
          fixFullscreenHtmlBannerUI(body, webLayout, webView);
        });

        // Ensure WebView is scrollable
        webView.setOnTouchListener((v, event) -> {
          if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
            v.getParent().requestDisallowInterceptTouchEvent(true);
          }
          return false;
        });

        fixFullscreenHtmlBannerUI(body, webLayout, webView);

        String encodedHtml = null;
        try {
          encodedHtml = Base64.encodeToString(htmlWithJs.getBytes("UTF-8"), Base64.NO_PADDING);
        } catch (UnsupportedEncodingException e) {
          Logger.e(TAG, "composeHtmlBanner AppBanner UnsupportedEncodingException.", e);
        }
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(encodedHtml, "text/html; charset=utf-8", "base64");

        body.addView(webLayout);
      });
    } catch (Exception e) {
      Logger.e(TAG, "Error in AppBanner composeHtmlBanner.", e);
    }
  }

  public static int pxToDp(int px) {
    return (int) (px * Resources.getSystem().getDisplayMetrics().density);
  }

  public class CleverpushInterface {
    @JavascriptInterface
    public void subscribe() {
      CleverPush.getInstance(CleverPush.context).subscribe();
    }

    @JavascriptInterface
    public void unsubscribe() {
      CleverPush.getInstance(CleverPush.context).unsubscribe();
    }

    @JavascriptInterface
    public void closeBanner() {
      appBannerPopup.dismiss();
    }

    @JavascriptInterface
    public void trackEventStringified(String eventID, String propertiesJSON) {
      try {
        Map<String, Object> properties = null;
        if (propertiesJSON != null) {
          properties = new Gson().fromJson(propertiesJSON, Map.class);
        }
        CleverPush.getInstance(CleverPush.context).trackEvent(eventID, properties);
      } catch (Exception ex) {
        Logger.e(TAG, "Error in AppBanner's HTML trackEvent error.", ex);
      }
    }

    @JavascriptInterface
    public void trackClickStringified(String buttonId, String customDataJSON) {
      CleverPush cleverPush = CleverPush.getInstance(CleverPush.context);
      try {
        Map<String, Object> customData = null;
        if (customDataJSON != null) {
          customData = new Gson().fromJson(customDataJSON, Map.class);
        }

        BannerAction bannerAction = BannerAction.create("html", customData);
        if (cleverPush.getAppBannerOpenedListener() != null) {
          cleverPush.getAppBannerOpenedListener().opened(bannerAction);
        }
      } catch (Exception ex) {
        Logger.e(TAG, "Error in AppBanner's HTML trackClick error.", ex);
      }

      cleverPush.getAppBannerModule()
          .sendBannerEvent("clicked", appBannerPopup.getData(), buttonId, null);
    }

    @JavascriptInterface
    public void openWebView(String url) {
      WebViewActivity.launch(activity, url);
    }

    @JavascriptInterface
    public void setSubscriptionAttribute(String attributeID, String value) {
      CleverPush.getInstance(CleverPush.context).setSubscriptionAttribute(attributeID, value);
    }

    @JavascriptInterface
    public String getSubscriptionAttribute(String attributeID) {
      try {
        Object attributeValue = CleverPush.getInstance(CleverPush.context).getSubscriptionAttribute(attributeID);
        return attributeValue != null ? attributeValue.toString() : "";
      } catch (Exception ex) {
        Logger.e(TAG, "Error while retrieving subscription attribute.", ex);
        return "";
      }
    }

    @JavascriptInterface
    public void addSubscriptionTag(String tagId) {
      CleverPush.getInstance(CleverPush.context).addSubscriptionTag(tagId);
    }

    @JavascriptInterface
    public void removeSubscriptionTag(String tagId) {
      CleverPush.getInstance(CleverPush.context).removeSubscriptionTag(tagId);
    }

    @JavascriptInterface
    public void setSubscriptionTopics(String[] topicIds) {
      CleverPush.getInstance(CleverPush.context).setSubscriptionTopics(topicIds);
    }

    @JavascriptInterface
    public void addSubscriptionTopic(String topicId) {
      CleverPush.getInstance(CleverPush.context).addSubscriptionTopic(topicId);
    }

    @JavascriptInterface
    public void removeSubscriptionTopic(String topicId) {
      CleverPush.getInstance(CleverPush.context).removeSubscriptionTopic(topicId);
    }

    @JavascriptInterface
    public void showTopicsDialog() {
      CleverPush.getInstance(CleverPush.context).showTopicsDialog();
    }

    @JavascriptInterface
    public void goToScreen(String screenId) {
      try {
        activity.runOnUiThread(() -> {
          for (int i = 0; i < screens.size(); i++) {
            if (screens.get(i).getId() != null && screens.get(i).getId().equals(screenId)) {
              appBannerPopup.moveToNextScreen(i);
              break;
            }
          }
        });
      } catch (Exception e) {
        Logger.e(TAG, "Error while performing goToScreen in HTML banner. " + e.getLocalizedMessage(), e);
      }
    }

    @JavascriptInterface
    public void nextScreen() {
      activity.runOnUiThread(() -> {
        appBannerPopup.moveToNextScreen();
      });
    }

    @JavascriptInterface
    public void previousScreen() {
      activity.runOnUiThread(() -> {
        appBannerPopup.moveToPreviousScreen();
      });
    }

    @JavascriptInterface
    public void handleLinkBySystem(String link) {
      activity.runOnUiThread(() -> {
        if (link != null && !link.trim().isEmpty()) {
          try {
            Uri uri = Uri.parse(link.trim());
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            activity.startActivity(intent);
          } catch (Exception e) {
            Logger.e(TAG, "App-Banner handleLinkBySystem: Failed to handle link → " + link, e);
          }
        } else {
          Logger.i(TAG, "App-Banner handleLinkBySystem: Invalid link (null or empty).");
        }
      });
    }
  }

  @JavascriptInterface
  public void copyToClipboard(String text) {
    ClipboardManager clipboard = (ClipboardManager) CleverPush.context.getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      ClipData clip = ClipData.newPlainText("label", text);
      clipboard.setPrimaryClip(clip);
    }
  }

  /**
   * Checks if the banner position type is set to "full."
   *
   * @return True if the banner position type is "full," false otherwise.
   */
  private boolean isBannerPositionFull() {
    if (data.getPositionType().equalsIgnoreCase(POSITION_TYPE_FULL)) {
      return true;
    }
    return false;
  }
}
