package com.cleverpush.stories.listener;

import android.app.Activity;
import android.os.Handler;
import android.view.View;
import android.webkit.JavascriptInterface;

import com.cleverpush.stories.StoryDetailViewHolder;

public class StoryDetailJavascriptInterface {

    private StoryDetailViewHolder storyDetailViewHolder;
    private StoryChangeListener storyChangeListener;
    private Activity activity;

    public StoryDetailJavascriptInterface(StoryDetailViewHolder storyDetailViewHolder, StoryChangeListener storyChangeListener, Activity activity) {
        this.storyDetailViewHolder = storyDetailViewHolder;
        this.storyChangeListener = storyChangeListener;
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                activity.runOnUiThread(() -> storyDetailViewHolder.progressBar.setVisibility(View.GONE));
            }
        },4500);

    }

}
