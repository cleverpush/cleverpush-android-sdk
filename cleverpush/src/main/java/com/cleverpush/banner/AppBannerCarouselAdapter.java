package com.cleverpush.banner;

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
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;

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
    private final Activity activity;
    private final Banner data;
    private final List<BannerScreens> screens = new LinkedList<>();
    private final AppBannerPopup appBannerPopup;

    static {
        alignmentMap.put(Alignment.Left, View.TEXT_ALIGNMENT_TEXT_START);
        alignmentMap.put(Alignment.Center, View.TEXT_ALIGNMENT_CENTER);
        alignmentMap.put(Alignment.Right, View.TEXT_ALIGNMENT_TEXT_END);
    }

    AppBannerCarouselAdapter(Activity activity, Banner banner, AppBannerPopup appBannerPopup, AppBannerOpenedListener openedListener) {
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
        @SuppressLint("InflateParams") Button button = (Button) activity.getLayoutInflater().inflate(R.layout.app_banner_button, null);
        button.setText(block.getText());
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4 / 3);
        button.setTextColor(ColorUtils.parseColor(block.getColor()));
        Integer alignment = alignmentMap.get(block.getAlignment());
        button.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(block.getRadius() * getPXScale());
        bg.setColor(ColorUtils.parseColor(block.getBackground()));
        button.setBackground(bg);

        if (block.getAction() != null) {
            button.setOnClickListener(view -> this.onClickListener(block.getAction()));
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
        @SuppressLint("InflateParams") TextView textView = (TextView) activity.getLayoutInflater().inflate(R.layout.app_banner_text, null);
        textView.setText(block.getText());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4 / 3);
        textView.setTextColor(ColorUtils.parseColor(block.getColor()));

        if (block.getFamily() != null) {
            try {
                Typeface font = Typeface.createFromAsset(activity.getAssets(), block.getFamily() + ".ttf");
                textView.setTypeface(font);
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }

        }

        Integer alignment = alignmentMap.get(block.getAlignment());
        textView.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

        body.addView(textView);
    }

    private void composeImageBlock(LinearLayout body, BannerImageBlock block, int position) {
        @SuppressLint("InflateParams") ConstraintLayout imageLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_image, null);
        ImageView img = imageLayout.findViewById(R.id.imageView);

        ConstraintSet imgConstraints = new ConstraintSet();
        imgConstraints.clone(imageLayout);
        float widthPercentage = Math.min(100, Math.max(0, block.getScale())) / 100.0f;
        imgConstraints.constrainPercentWidth(img.getId(), widthPercentage);
        imgConstraints.applyTo(imageLayout);

        body.addView(imageLayout);

        new Thread(() -> {
            try {
                InputStream in = new URL(block.getImageUrl()).openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                if (bitmap != null) {
                    img.setImageBitmap(bitmap);
                }
            } catch (Exception ignored) {

            }
        }).start();

        if (block.getAction() != null) {
            img.setOnClickListener(view -> this.onClickListener(block.getAction()));
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void composeHtmlBLock(LinearLayout body, BannerHTMLBlock block) {
        @SuppressLint("InflateParams") LinearLayout webLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html_block, null);
        WebView webView = webLayout.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.loadUrl(block.getUrl());
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

    /**
     * Will compose and add HTML Banner to the body of banner layout.
     *
     * @param body        parent layout to add HTML view
     * @param htmlContent html content which will be displayed in banner
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void composeHtmlBanner(LinearLayout body, String htmlContent) {
        activity.runOnUiThread(() -> {
            String htmlWithJs = htmlContent.replace("</body>", "" +
                    "<script type=\"text/javascript\">\n" +
                    "// Below conditions will take care of all ids and classes which contains defined keywords at start and end of string\n" +
                    "var closeBtns = document.querySelectorAll('[id^=\"close\"], [id$=\"close\"], [class^=\"close\"], [class$=\"close\"]');\n" +
                    "function onCloseClick() {\n" +
                    "  try {\n" +
                    "    htmlBannerInterface.close();\n" +
                    "  } catch (error) {\n" +
                    "    console.log('Caught error on closeBtn click', error);\n" +
                    "  }\n" +
                    "}\n" +
                    "for (var i = 0; i < closeBtns.length; i++) {\n" +
                    "  closeBtns[i].addEventListener('click', onCloseClick);\n" +
                    "}\n" +
                    "</script>\n" +
                    "</body>");
            ConstraintLayout webLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html, null);
            WebView webView = webLayout.findViewById(R.id.webView);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setLoadsImagesAutomatically(true);
            webView.addJavascriptInterface(new HtmlBannerJavascriptInterface(), "htmlBannerInterface");

            String encodedHtml = null;
            try {
                encodedHtml = Base64.encodeToString(htmlWithJs.getBytes("UTF-8"), Base64.NO_PADDING);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            webView.loadData(encodedHtml, "text/html; charset=utf-8", "UTF-8");

            body.addView(webLayout);
        });
    }

    public static int pxToDp(int px) {
        return (int) (px * Resources.getSystem().getDisplayMetrics().density);
    }

    public class HtmlBannerJavascriptInterface {
        @JavascriptInterface
        public void close() {
            appBannerPopup.dismiss();
        }
    }
}
