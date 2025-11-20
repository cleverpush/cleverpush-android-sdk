package com.cleverpush.banner;

import android.content.Intent;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cleverpush.util.Logger;

public class AppBannerWebViewClient extends WebViewClient {
  public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    String GET_METHOD = "GET";
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && request.isForMainFrame() && request.getUrl() != null
              && request.getMethod().equals(GET_METHOD)) {
        view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
        return true;
      }
    } catch (Exception e) {
      Logger.e("CleverPush", "Failed to override URL loading in AppBannerWebViewClient", e);
    }
    return false;
  }
}
