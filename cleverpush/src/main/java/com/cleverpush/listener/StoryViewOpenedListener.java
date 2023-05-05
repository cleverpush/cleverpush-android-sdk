package com.cleverpush.listener;

import android.webkit.WebResourceRequest;

import java.io.Serializable;

public interface StoryViewOpenedListener extends Serializable {
  void opened(WebResourceRequest request);
}
