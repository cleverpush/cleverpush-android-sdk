package com.cleverpush.stories.listener;

import android.view.GestureDetector;
import android.view.MotionEvent;

public class GestureListener extends GestureDetector.SimpleOnGestureListener {
  private static final int SWIPE_THRESHOLD = 150;
  private static final int SWIPE_VELOCITY_THRESHOLD = 150;
  private OnSwipeDownListener onSwipeDownListener;

  public GestureListener(OnSwipeDownListener onSwipeDownListener) {
    this.onSwipeDownListener = onSwipeDownListener;
  }

  @Override
  public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
    float diffY = e2.getY() - e1.getY();
    float diffX = e2.getX() - e1.getX();
    if (Math.abs(diffY) > Math.abs(diffX) && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
      if (diffY > 0) {
        onSwipeDownListener.onSwipeDown();
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean onSingleTapConfirmed(MotionEvent e) {
    return false;
  }
}
