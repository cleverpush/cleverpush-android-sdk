package com.cleverpush.stories.listener;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.webkit.JavascriptInterface;

import com.cleverpush.listener.StoryViewOpenedListener;
import com.cleverpush.stories.StoryDetailActivity;
import com.cleverpush.stories.StoryDetailListAdapter;
import com.cleverpush.util.Logger;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StoryDetailJavascriptInterface {
  private StoryChangeListener storyChangeListener;
  private Activity activity;
  StoryViewOpenedListener storyViewOpenedListener;
  String currentTimeStamp = "";
  StoryDetailListAdapter storyDetailListAdapter;

  public StoryDetailJavascriptInterface(StoryChangeListener storyChangeListener, Activity activity, StoryViewOpenedListener storyViewOpenedListener, StoryDetailListAdapter storyDetailListAdapter) {
    this.storyChangeListener = storyChangeListener;
    this.storyViewOpenedListener = storyViewOpenedListener;
    this.activity = activity;
    this.storyDetailListAdapter = storyDetailListAdapter;
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
  public void noNext() {
    storyChangeListener.noNext();
  }

  @JavascriptInterface
  public void navigation(int position) {
    storyChangeListener.onNavigation(position);
  }

  @JavascriptInterface
  public void storyNavigation(int position, int subStoryIndex) {
    storyChangeListener.onStoryNavigation(position, subStoryIndex);
  }

  @JavascriptInterface
  public void storyButtonCallbackUrl(String data) {
    try {
      String previousTimeStamp = currentTimeStamp;
      currentTimeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(new Date());

      boolean shouldPerformAction = false;

      if (previousTimeStamp.isEmpty()) {
        shouldPerformAction = true;
      } else {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());
        Date previousDate = sdf.parse(previousTimeStamp);
        Date currentDate = sdf.parse(currentTimeStamp);

        long diffInMilliseconds = currentDate.getTime() - previousDate.getTime();

        if (diffInMilliseconds > 350) {
          shouldPerformAction = true;
        }
      }
      if (shouldPerformAction) {
        if (data != null && !data.isEmpty()) {
          JSONObject jsonObject = new JSONObject(data);
          String url = jsonObject.optString("callbackUrl");

          if (url != null && !url.isEmpty()) {
            Uri uri = Uri.parse(url);
            StoryDetailActivity.isOpenFromButton = true;
            if (storyViewOpenedListener != null) {
              storyViewOpenedListener.opened(uri);
            } else {
              if (uri != null) {
                activity.startActivity(new Intent(Intent.ACTION_VIEW, uri));
              }
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
