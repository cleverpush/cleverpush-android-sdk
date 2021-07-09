package com.cleverpush.stories.listener;

import android.webkit.JavascriptInterface;

import com.cleverpush.stories.listener.StoryChangeListener;

public class StoryDetailJavascriptInterface {

    @JavascriptInterface
    public void next(int position, StoryChangeListener storyChangeListener) {
        storyChangeListener.onNext(position);
    }

    @JavascriptInterface
    public void previous(int position, StoryChangeListener storyChangeListener) {
        storyChangeListener.onPrevious(position);
    }

    @JavascriptInterface
    public void ready(int position, StoryChangeListener storyChangeListener) {
        storyChangeListener.onReady(position);
    }

}
