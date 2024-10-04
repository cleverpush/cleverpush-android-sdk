package com.cleverpush.stories.listener;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class OnSwipeTouchListener implements View.OnTouchListener {

  private GestureDetector gestureDetector;

  public OnSwipeTouchListener(Context ctx, OnSwipeDownListener onSwipeDownListener) {
    gestureDetector = new GestureDetector(ctx, new GestureListener(onSwipeDownListener));
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  public GestureDetector getGestureDetector() {
    return this.gestureDetector;
  }

}
