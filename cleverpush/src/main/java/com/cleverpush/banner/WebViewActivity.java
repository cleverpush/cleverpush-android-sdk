package com.cleverpush.banner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
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
    setContentView(R.layout.activity_web_view);
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
      WebView webView = findViewById(R.id.webView);
      ImageView closeButton = findViewById(R.id.ivClose);

      if (webView == null) {
        Logger.e("CleverPush", "WebViewActivity: WebView is null");
        return;
      }

      webView.getSettings().setJavaScriptEnabled(true);
      webView.loadUrl(url);

      if (closeButton == null) {
        Logger.e("CleverPush","WebViewActivity: Close button is null");
        return;
      }

      closeButton.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          finish();
        }
      });
    } catch (Exception e) {
      Logger.e("CleverPush", "Error while setting webView. " + e.getLocalizedMessage(), e);
    }
  }
}
