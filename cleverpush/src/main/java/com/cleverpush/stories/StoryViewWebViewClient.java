package com.cleverpush.stories;

import android.content.Intent;
import android.os.Build;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.cleverpush.listener.StoryViewOpenedListener;

public class StoryViewWebViewClient  extends WebViewClient {

  public StoryViewOpenedListener storyViewOpenedListener;

  StoryViewWebViewClient(StoryViewOpenedListener storyViewOpenedListener) {
    this.storyViewOpenedListener = storyViewOpenedListener;
  }

  public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    String GET_METHOD = "GET";
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      if (storyViewOpenedListener != null) {
        storyViewOpenedListener.opened(request);
        return true;
      } else {
        if (request.isForMainFrame() && request.getUrl() != null
                && request.getMethod().equals(GET_METHOD)) {
          view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
          return true;
        }
      }
    }
    return false;
  }

}
