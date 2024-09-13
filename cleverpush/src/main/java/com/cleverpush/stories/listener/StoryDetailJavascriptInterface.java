package com.cleverpush.stories.listener;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.stories.StoryDetailViewHolder;
import com.cleverpush.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class StoryDetailJavascriptInterface {

  private StoryDetailViewHolder storyDetailViewHolder;
  private StoryChangeListener storyChangeListener;
  private Activity activity;
  StoryViewOpenedListener storyViewOpenedListener;

  public StoryDetailJavascriptInterface(StoryDetailViewHolder storyDetailViewHolder,
                                        StoryChangeListener storyChangeListener, Activity activity, StoryViewOpenedListener storyViewOpenedListener) {
    this.storyDetailViewHolder = storyDetailViewHolder;
    this.storyChangeListener = storyChangeListener;
    this.storyViewOpenedListener = storyViewOpenedListener;
    this.activity = activity;
  }

  @JavascriptInterface
  public void next(int position) {
    storyChangeListener.onNext(position);
  }

  @JavascriptInterface
  public void previous(int position) {
    storyChangeListener.onPrevious(position);
  }

  @JavascriptInterface
  public void ready() {

  }

  @JavascriptInterface
  public void storyNavigation(int position, int subStoryIndex) {
    storyChangeListener.onStoryNavigation(position, subStoryIndex);
  }

  @JavascriptInterface
  public void storyButtonCallbackUrl(String data) {
    try {
      if (data != null && !data.isEmpty()) {
        JSONObject jsonObject = new JSONObject(data);
        String url = jsonObject.optString("callbackUrl");

        if (url != null && !url.isEmpty()) {
          Uri uri = Uri.parse(url);
          if (storyViewOpenedListener != null) {
            storyViewOpenedListener.opened(uri);
          } else {
            if (uri != null) {
              activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
            }
          }
        }
      }
    } catch (JSONException e) {
      Logger.e("CleverPush", "Invalid JSON format in storyButtonCallback data: " + data, e);
    } catch (Exception e) {
      Logger.e("CleverPush", "Error while opening StoryView URL: " + data, e);
    }
  }
}
