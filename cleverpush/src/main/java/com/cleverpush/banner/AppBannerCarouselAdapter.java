package com.cleverpush.banner;

import static com.cleverpush.Constants.LOG_TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.util.Base64;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
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
import com.google.gson.Gson;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class AppBannerCarouselAdapter extends RecyclerView.Adapter<AppBannerCarouselAdapter.ViewHolder> {

  private static final String TAG = "CleverPush/AppBanner";
  private static final String CONTENT_TYPE_HTML = "html";
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

      HashMap<String, String> currentVoucherCodePlaceholder = CleverPush.getInstance(CleverPush.context).getAppBannerModule().getCurrentVoucherCodePlaceholder();
      if (currentVoucherCodePlaceholder != null && currentVoucherCodePlaceholder.containsKey(data.getId())) {
        voucherCode = currentVoucherCodePlaceholder.get(data.getId());
      }

      if (data.getContentType() != null && data.getContentType().equalsIgnoreCase(CONTENT_TYPE_HTML)) {
        composeHtmlBanner(body, data.getContent());
      } else {
        for (BannerBlock bannerBlock : data.getScreens().get(position).getBlocks()) {
          activity.runOnUiThread(() -> {
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
          });
        }
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "onBindViewHolder Exception: " + e.getLocalizedMessage());
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

  private void onClickListener(BannerAction action) {
    if (action.isOpenInWebView() && action.getUrl() != null && !action.getUrl().isEmpty()) {
      WebViewActivity.launch(activity, action.getUrl());
    } else if (action.getScreen() != null && !action.getScreen().isEmpty()) {
      for (int i = 0; i < screens.size(); i++) {
        if (screens.get(i).getId() != null && screens.get(i).getId().equals(action.getScreen())) {
          appBannerPopup.moveToNextScreen(i);
          break;
        }
      }
    } else if (action.getDismiss()) {
      appBannerPopup.dismiss();
    } else {
      appBannerPopup.moveToNextScreen();
    }

    if (action.isOpenBySystem() && action.getUrl() != null && !action.getUrl().isEmpty()) {
      activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(action.getUrl())));
    }

    if (appBannerPopup.getOpenedListener() != null) {
      appBannerPopup.getOpenedListener().opened(action);
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  private void composeButtonBlock(LinearLayout body, BannerButtonBlock block, int position) {
    @SuppressLint("InflateParams") Button button =
        (Button) activity.getLayoutInflater().inflate(R.layout.app_banner_button, null);
    String text = block.getText();
    if (voucherCode != null && text.contains("{voucherCode}")) {
      text = text.replace("{voucherCode}", voucherCode);
    }
    button.setText(text);
    button.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4 / 3);

    if (block.getFamily() != null) {
      try {
        Typeface font = FontUtils.findFont(activity, block.getFamily());
        button.setTypeface(font);
      } catch (Exception ex) {
        Logger.e(TAG, ex.getMessage(), ex);
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
        LinearLayout.LayoutParams.MATCH_PARENT
    );
    params.setMargins(0, 20, 0, 20);
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
      action.setBlockId(block.getId());
      button.setOnClickListener(view -> this.onClickListener(action));
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
    String text = block.getText();
    if (voucherCode != null && text.contains("{voucherCode}")) {
      text = text.replace("{voucherCode}", voucherCode);
    }
    textView.setText(text);
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
        Logger.e(TAG, ex.getMessage(), ex);
      }
    }

    Integer alignment = alignmentMap.get(block.getAlignment());
    textView.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

    Integer gravity = gravityMap.get(block.getAlignment());
    textView.setGravity(gravity == null ? Gravity.CENTER : gravity);

    body.addView(textView);
  }

  private void composeImageBlock(LinearLayout body, BannerImageBlock block, int position) {
    @SuppressLint("InflateParams") ConstraintLayout imageLayout =
            (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_image, null);
    ImageView img = imageLayout.findViewById(R.id.imageView);
    ProgressBar progressBar = imageLayout.findViewById(R.id.progressBar);
    progressBar.setVisibility(View.VISIBLE);

    ConstraintSet imgConstraints = new ConstraintSet();
    imgConstraints.clone(imageLayout);

    float widthPercentage = Math.min(100, Math.max(0, block.getScale())) / 100.0f;
    imgConstraints.constrainPercentWidth(img.getId(), widthPercentage);
    float aspectRatio = 1.0f;
    float height = widthPercentage / aspectRatio * 100;
    imgConstraints.constrainPercentHeight(img.getId(), height);

    imgConstraints.applyTo(imageLayout);

    new Thread(() -> {
      try {
        String imageUrl;
        if (appBannerPopup.getData().isDarkModeEnabled(activity) && block.getDarkImageUrl() != null) {
          imageUrl = block.getDarkImageUrl();
        } else {
          imageUrl = block.getImageUrl();
        }

        InputStream in = new URL(imageUrl).openStream();
        Bitmap bitmap = BitmapFactory.decodeStream(in);
        activity.runOnUiThread(() -> {
          if (bitmap != null) {
            img.setImageBitmap(bitmap);
          }
          progressBar.setVisibility(View.GONE);
        });

      } catch (Exception ignored) {
        Logger.d(TAG, ignored.getLocalizedMessage());
        if (activity != null) {
          activity.runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
          });
        }
      }
    }).start();

    body.addView(imageLayout);

    if (block.getAction() != null) {
      BannerAction action = block.getAction();
      action.setBlockId(block.getId());
      img.setOnClickListener(view -> this.onClickListener(action));
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void composeHtmlBLock(LinearLayout body, BannerHTMLBlock block) {
    @SuppressLint("InflateParams") LinearLayout webLayout =
        (LinearLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html_block, null);
    WebView webView = webLayout.findViewById(R.id.webView);
    webView.getSettings().setJavaScriptEnabled(true);
    webView.setVerticalScrollBarEnabled(false);
    webView.setHorizontalScrollBarEnabled(false);
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
    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.constrainMinHeight(R.id.webView, Resources.getSystem().getDisplayMetrics().heightPixels);
    constraintSet.applyTo(webLayout);
    body.setPadding(0, 0, 0, 0);
    ViewGroup.LayoutParams layoutParams = webView.getLayoutParams();
    layoutParams.width = Resources.getSystem().getDisplayMetrics().widthPixels;
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
        String html = htmlContent;
        if (voucherCode != null && html.contains("{voucherCode}")) {
          html = html.replace("{voucherCode}", voucherCode);
        }
        String htmlWithJs = html.replace("</body>", "" +
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
                "</script>\n" +
                "</body>");
        ConstraintLayout webLayout =
                (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html, null);
        WebView webView = webLayout.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadsImagesAutomatically(true);
        webView.addJavascriptInterface(new CleverpushInterface(), "CleverPush");
        webView.setWebViewClient(new AppBannerWebViewClient());

        fixFullscreenHtmlBannerUI(body, webLayout, webView);

        String encodedHtml = null;
        try {
          encodedHtml = Base64.encodeToString(htmlWithJs.getBytes("UTF-8"), Base64.NO_PADDING);
        } catch (UnsupportedEncodingException e) {
          Logger.d(LOG_TAG, "AppBanner UnsupportedEncodingException");
          e.printStackTrace();
        }
        webView.setBackgroundColor(Color.TRANSPARENT);
        webView.loadData(encodedHtml, "text/html; charset=utf-8", "base64");

        body.addView(webLayout);
      });
    } catch (Exception e) {
      Logger.e(TAG, "composeHtmlBanner Exception: " + e.getLocalizedMessage());
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
        Logger.e(LOG_TAG, "trackEvent error " + ex.getMessage());
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
        Logger.e(LOG_TAG, "trackClick error " + ex.getMessage());
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
  }
}
