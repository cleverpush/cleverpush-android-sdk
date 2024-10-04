package com.cleverpush.stories.listener;

public interface StoryChangeListener {

  void onNext(int position);

  void onPrevious(int position);

  void onStoryNavigation(int position, int subStoryPosition);

  void onNavigation(int position);

  void noNext();
}
