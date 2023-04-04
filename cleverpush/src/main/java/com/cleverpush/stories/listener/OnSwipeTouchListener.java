package com.cleverpush.stories.listener;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

public class OnSwipeTouchListener implements OnTouchListener {

  private GestureDetector gestureDetector;

  public OnSwipeTouchListener(Context ctx, View mainView, OnSwipeDownListener onSwipeDownListener) {
    this.gestureDetector = new GestureDetector(ctx, new GestureListener(onSwipeDownListener));
    mainView.setOnTouchListener(this);
  }

  @Override
  public boolean onTouch(View v, MotionEvent event) {
    return gestureDetector.onTouchEvent(event);
  }

  public GestureDetector getGestureDetector() {
    return this.gestureDetector;
  }

}
