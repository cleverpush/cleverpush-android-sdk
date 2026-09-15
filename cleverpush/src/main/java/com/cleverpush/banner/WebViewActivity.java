package com.cleverpush.banner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import com.cleverpush.R;
import com.cleverpush.util.Logger;

public class WebViewActivity extends Activity {

  private String url;

  public static void launch(Activity activity, String url) {
    Intent intent = new Intent(activity, WebViewActivity.class);
    intent.putExtra("url", url);
    activity.startActivity(intent);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.cp_activity_web_view);
    handleBundleData(getIntent().getExtras());
  }

  private void handleBundleData(Bundle extras) {
    try {
      if (extras.containsKey("url")) {
        url = extras.getString("url");
        init();
      }
    } catch (Exception e) {
      Logger.e("CleverPush", "WebViewActivity handleBundleData Exception: " + e.getLocalizedMessage(), e);
    }
  }

  @SuppressLint("SetJavaScriptEnabled")
  private void init() {
    try {
      WebView webView = findViewById(R.id.cp_webview);
      ImageView closeButton = findViewById(R.id.cp_iv_close);

      if (webView == null) {
        Logger.e("CleverPush", "WebViewActivity: WebView is null");
        return;
      }

      WebSettings settings = webView.getSettings();
      settings.setJavaScriptEnabled(true);
      settings.setLoadsImagesAutomatically(true);
      settings.setDomStorageEnabled(true);
      settings.setAllowFileAccess(true);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
      }

      CookieManager.getInstance().setAcceptCookie(true);

      webView.setWebViewClient(new WebViewClient() {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
          return false;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
          return false;
        }
      });

      webView.loadUrl(url);

      if (closeButton == null) {
        Logger.e("CleverPush","WebViewActivity: Close button is null");
        return;
      }

      closeButton.setOnClickListener(view -> finish());
    } catch (Exception e) {
      Logger.e("CleverPush", "Error while setting webView. " + e.getLocalizedMessage(), e);
    }
  }
}
