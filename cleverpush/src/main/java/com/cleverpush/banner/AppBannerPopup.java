package com.cleverpush.banner;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.cleverpush.R;
import com.cleverpush.banner.models.Banner;
import com.cleverpush.banner.models.blocks.Alignment;
import com.cleverpush.banner.models.blocks.BannerBackground;
import com.cleverpush.banner.models.blocks.BannerBlock;
import com.cleverpush.banner.models.blocks.BannerButtonBlock;
import com.cleverpush.banner.models.blocks.BannerImageBlock;
import com.cleverpush.banner.models.blocks.BannerTextBlock;
import com.cleverpush.listener.AppBannerOpenedListener;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class AppBannerPopup {
    private static final String CONTENT_TYPE_BLOCKS = "block";
    private static final String CONTENT_TYPE_HTML = "html";

    private static SpringForce getDefaultForce(float finalValue) {
        SpringForce force = new SpringForce(finalValue);
        force.setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
        float DEFAULT_STIFFNESS_RATIO = 300f;
        force.setStiffness(DEFAULT_STIFFNESS_RATIO);

        return force;
    }
    private static Map<Alignment, Integer> alignmentMap = new HashMap<>();
    static {
        alignmentMap.put(Alignment.Left, View.TEXT_ALIGNMENT_TEXT_START);
        alignmentMap.put(Alignment.Center, View.TEXT_ALIGNMENT_CENTER);
        alignmentMap.put(Alignment.Right, View.TEXT_ALIGNMENT_TEXT_END);
    }

    private Handler mainHandler;

    private Activity activity;
    private Banner data;

    private PopupWindow popup;
    private View popupRoot;

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

    private float getFontScale() {
        float PT_TO_PX = 4.0f / 3.0f;

        return PT_TO_PX * getPXScale();
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
        if (isInitialized) { return; }

        popupRoot = createLayout();
        LinearLayout body =  popupRoot.findViewById(R.id.bannerBody);

        composeBackground(body);
        if (data.getContentType() != null && data.getContentType().equalsIgnoreCase(CONTENT_TYPE_HTML)) {
            composeHtmlBanner(body, data.getContent());
        }else if (data.getContentType().equalsIgnoreCase(CONTENT_TYPE_BLOCKS)) {
            for (BannerBlock bannerBlock : data.getBlocks()) {
                switch (bannerBlock.getType()) {
                    case Text:
                        composeTextBlock(body, (BannerTextBlock) bannerBlock);
                        break;
                    case Image:
                        composeImageBlock(body, (BannerImageBlock) bannerBlock);
                        break;
                    case Button:
                        composeButtonBlock(body, (BannerButtonBlock) bannerBlock);
                        break;
                    default:
                        throw new RuntimeException("Not implemented");
                }
            }
        } else {
            throw new RuntimeException("Not implemented");
        }

        popup = new PopupWindow(
            popupRoot,
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT,
            true
        );
        popup.setAnimationStyle(R.style.banner_animation);

        isInitialized = true;
    }

    public void show() {
        if(!isInitialized) {
            throw new IllegalStateException("Must be initialized");
        }

        runInMain(this::tryShowSafe);
    }

    private void tryShowSafe() {
        if(this.isRootReady()) {
            popupRoot.findViewById(R.id.bannerBody).setTranslationY(getRoot().getHeight());
            popup.showAtLocation(getRoot(), Gravity.CENTER, 0, 0);

            animateBody(getRoot().getHeight(), 0f);
        } else {
            runInMain(this::tryShowSafe, 20);
        }
    }

    public void dismiss() {
        if(!isInitialized) {
            throw new IllegalStateException("Must be initialized");
        }

        runInMain(() -> animateBody(0f, getRoot().getHeight()));
        runInMain(() -> popup.dismiss(), 200);
    }

    private View createLayout() {
        View layout = activity.getLayoutInflater().inflate(R.layout.app_banner, null);
        layout.setOnClickListener(view -> dismiss());

        return layout;
    }

    private void composeBackground(View body) {
        BannerBackground bg = data.getBackground();
        GradientDrawable drawableBG = new GradientDrawable();
        drawableBG.setShape(GradientDrawable.RECTANGLE);
        drawableBG.setCornerRadius(10 * getPXScale());
        drawableBG.setColor(this.parseColor(bg.getColor()));

        body.setBackground(drawableBG);
    }

    private void composeButtonBlock(LinearLayout body, BannerButtonBlock block) {
        Button button = (Button) activity.getLayoutInflater().inflate(R.layout.app_banner_button, null);
        button.setText(block.getText());
        button.setTextSize(TypedValue.COMPLEX_UNIT_PX, block.getSize() * getFontScale());
        button.setTextColor(this.parseColor(block.getColor()));
        Integer alignment = alignmentMap.get(block.getAlignment());
        button.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(block.getRadius() * getPXScale());
        bg.setColor(this.parseColor(block.getBackground()));
        button.setBackground(bg);

		button.setOnClickListener(view -> {
			if (block.getAction().getDismiss()) {
				dismiss();
			}

			if (this.openedListener != null) {
				this.openedListener.opened((block.getAction()));
			}
		});
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

    private void composeTextBlock(LinearLayout body, BannerTextBlock block) {
        TextView textView = (TextView) activity.getLayoutInflater().inflate(R.layout.app_banner_text, null);
        textView.setText(block.getText());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, block.getSize() * getFontScale());
        textView.setTextColor(this.parseColor(block.getColor()));

        Integer alignment = alignmentMap.get(block.getAlignment());
        textView.setTextAlignment(alignment == null ? View.TEXT_ALIGNMENT_CENTER : alignment);

        body.addView(textView);
    }

    private void composeImageBlock(LinearLayout body, BannerImageBlock block) {
        ConstraintLayout imageLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_image, null);
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
    }
    /**
     * Will compose and add HTML Banner to the body of banner layout.
     * @param  body  parent layout to add HTML view
     * @param  htmlContent html content which will be displayed in banner
     */
    private void composeHtmlBanner(LinearLayout body,  String htmlContent) {
        activity.runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
            @Override
            public void run() {
                if(htmlContent.endsWith("</body></html>")){
                    htmlContent.replace("</body></html>","");
                }
                String htmlWithJAVAScript=htmlContent.concat("<script type=\"text/javascript\">\n" +
                        "\t// Below conditions will take care of all ids and classes which contains defined keywords at start and end of string\n" +
                        "\tconst keyword = 'close';\n" +
                        "\tconst closeTagsIDAtStart = document.querySelectorAll(`[id^=\"${keyword}\"]`);\n" +
                        "\tconst closeTagsIDAtEnd = document.querySelectorAll(`[id$=\"${keyword}\"]`);\n" +
                        "\tconst closeTagsClassAtStart = document.querySelectorAll(`[class^=\"${keyword}\"]`);\n" +
                        "\tconst closeTagsClassAtEnd = document.querySelectorAll(`[class$=\"${keyword}\"]`);\n" +
                        "\tfunction onCloseClick() {\n" +
                        "\t\ttry {\n" +
                        "\t\t\thtmlBannerInterface.close();\n" +
                        "\t\t} catch (error) {\n" +
                        "\t\t\tconsole.log('Caught error on closeBTN click', error);\n" +
                        "\t\t}\n" +
                        "\t}\n" +
                        "\tcloseTagsIDAtStart.forEach(function(item) {\n" +
                        "\t\tconsole.log('atStart',item)\n" +
                        "\t\titem.addEventListener('click',onCloseClick);\n" +
                        "\t})\n" +
                        "\tcloseTagsIDAtEnd.forEach(function(item) {\n" +
                        "\t\tconsole.log('atEnd',item)\n" +
                        "\t\titem.addEventListener('click',onCloseClick);\n" +
                        "\t})\n" +
                        "\tcloseTagsClassAtStart.forEach(function(item) {\n" +
                        "\t\tconsole.log('atEnd',item)\n" +
                        "\t\titem.addEventListener('click',onCloseClick);\n" +
                        "\t})\n" +
                        "\tcloseTagsClassAtEnd.forEach(function(item) {\n" +
                        "\t\tconsole.log('atEnd',item)\n" +
                        "\t\titem.addEventListener('click',onCloseClick);\n" +
                        "\t})\n" +
                        "</script\n" +
                        "\n" +
                        "</body>\n" +
                        "</html>");
                ConstraintLayout webLayout = (ConstraintLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html, null);
                WebView webView = webLayout.findViewById(R.id.webView);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.addJavascriptInterface(new HtmlBannerJavascriptInterface(), "htmlBannerInterface");
                String encodedHtml = Base64.encodeToString(htmlWithJAVAScript.getBytes(), Base64.CRLF);
                webView.loadData(encodedHtml , "text/html", "base64");
                body.addView(webLayout);
            }
        });
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
        if(delay <= 0L) {
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

    /**
     * Will provide javascript bridge to perform close button click in HTML.
     */
    public class HtmlBannerJavascriptInterface {
        @JavascriptInterface
        public void close() {
            dismiss();
        }
    }
}
