package com.cleverpush.stories.listener;

public interface StoryChangeListener {

  void onNext(int position);

  void onPrevious(int position);

}
