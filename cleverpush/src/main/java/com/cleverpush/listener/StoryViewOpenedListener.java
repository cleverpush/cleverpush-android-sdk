package com.cleverpush.listener;

import android.net.Uri;

import java.io.Serializable;

public interface StoryViewOpenedListener extends Serializable {
  void opened(Uri request);
}
