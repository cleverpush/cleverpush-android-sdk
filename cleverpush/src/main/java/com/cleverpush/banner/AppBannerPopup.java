package com.cleverpush.banner;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
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
import android.widget.ScrollView;
import android.widget.TextView;

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
import com.cleverpush.banner.models.blocks.BannerHTMLBlock;
import com.cleverpush.banner.models.blocks.BannerImageBlock;
import com.cleverpush.banner.models.blocks.BannerTextBlock;
import com.cleverpush.listener.AppBannerOpenedListener;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class AppBannerPopup {
    private static final String CONTENT_TYPE_BLOCKS = "block";
    private static final String CONTENT_TYPE_HTML = "html";

    private static final String POSITION_TYPE_TOP = "top";
    private static final String POSITION_TYPE_BOTTOM = "bottom";
    private static final String POSITION_TYPE_CENTER = "center";

    private static final String TAG = "CleverPush/AppBanner";

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
        if (isInitialized) {
        	return;
        }

        popupRoot = createLayout();
        LinearLayout body =  popupRoot.findViewById(R.id.bannerBody);

        ConstraintLayout mConstraintLayout  = (ConstraintLayout)popupRoot.findViewById(R.id.parent);
        ScrollView scrollView =  popupRoot.findViewById(R.id.scrollView);

        ConstraintSet set = new ConstraintSet();
        set.clone(mConstraintLayout);

        switch (data.getPositionType()){
            case POSITION_TYPE_TOP:
                set.connect(scrollView.getId(), ConstraintSet.TOP, mConstraintLayout.getId(), ConstraintSet.TOP, 20);
                break;
            case POSITION_TYPE_BOTTOM:
                set.connect(scrollView.getId(), ConstraintSet.BOTTOM, mConstraintLayout.getId(), ConstraintSet.BOTTOM, 20);
                break;
            default:
                set.connect(scrollView.getId(), ConstraintSet.TOP, mConstraintLayout.getId(), ConstraintSet.TOP, 0);
                set.connect(scrollView.getId(), ConstraintSet.BOTTOM, mConstraintLayout.getId(), ConstraintSet.BOTTOM, 0);
                break;
        }

        set.applyTo(mConstraintLayout);

        composeBackground(body);
        if (data.getContentType() != null && data.getContentType().equalsIgnoreCase(CONTENT_TYPE_HTML)) {
            composeHtmlBanner(body, data.getContent());
        } else {
            for (BannerBlock bannerBlock : data.getBlocks()) {
                activity.runOnUiThread(() -> {
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
						case HTML:
							composeHtmlBLock(body,(BannerHTMLBlock)bannerBlock);
							break;
					}
				});
            }
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
        if (!isInitialized) {
			return;
        }

        new tryShowSafe().execute();
    }

    private void tryShowSafe() {
        if (this.isRootReady()) {
            popupRoot.findViewById(R.id.bannerBody).setTranslationY(getRoot().getHeight());
            popup.showAtLocation(getRoot(), Gravity.CENTER, 0, 0);

            animateBody(getRoot().getHeight(), 0f);
        } else {
            runInMain(this::tryShowSafe, 20);
        }
    }

    public void dismiss() {
        if (!isInitialized) {
            return;
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
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4/3);
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
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, block.getSize() * 4/3);
        textView.setTextColor(this.parseColor(block.getColor()));

        if(block.getFamily() != null){
            try {
                Typeface font = Typeface.createFromAsset(activity.getAssets(), block.getFamily()+".ttf");
                textView.setTypeface(font);
            }catch (Exception ex) {
                Log.e(TAG, ex.getMessage(), ex);
            }

        }

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

    private void composeHtmlBLock(LinearLayout body, BannerHTMLBlock block) {
		LinearLayout webLayout = (LinearLayout) activity.getLayoutInflater().inflate(R.layout.app_banner_html_block, null);
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
        body.addView(webLayout);
    }

    public static int pxToDp(int px) {
        return (int) (px * Resources.getSystem().getDisplayMetrics().density);
    }
    /**
     * Will compose and add HTML Banner to the body of banner layout.
     * @param  body  parent layout to add HTML view
     * @param  htmlContent html content which will be displayed in banner
     */
    private void composeHtmlBanner(LinearLayout body, String htmlContent) {
        activity.runOnUiThread(() -> {
		String htmlWithJs = htmlContent.replace("</body>","" +
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
		webView.loadData(encodedHtml , "text/html; charset=utf-8", "UTF-8");

		body.addView(webLayout);
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

    public class tryShowSafe extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {
            if(isRootReady()) {
                return  true;
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
                popupRoot.findViewById(R.id.bannerBody).setTranslationY(getRoot().getHeight());
                popup.showAtLocation(getRoot(), Gravity.CENTER, 0, 0);

                animateBody(getRoot().getHeight(), 0f);
            }
        }

    }
}
