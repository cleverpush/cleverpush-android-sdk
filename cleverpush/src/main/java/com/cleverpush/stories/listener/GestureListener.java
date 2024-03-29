package com.cleverpush.stories.listener;

import static com.cleverpush.Constants.LOG_TAG;

import android.view.GestureDetector;
import android.view.MotionEvent;

import com.cleverpush.util.Logger;

public final class GestureListener extends GestureDetector.SimpleOnGestureListener {

  private static final int SWIPE_THRESHOLD = 100;
  private static final int SWIPE_VELOCITY_THRESHOLD = 100;

  private OnSwipeDownListener onSwipeDownListener;

  public GestureListener(OnSwipeDownListener onSwipeDownListener) {
    this.onSwipeDownListener = onSwipeDownListener;
  }

  @Override
  public boolean onDown(MotionEvent e) {
    return true;
  }

  @Override
  public boolean onFling(MotionEvent motionEvent, MotionEvent secondMotionEvent, float velocityX, float velocityY) {
    boolean result = false;
    try {
      float diffY = secondMotionEvent.getY() - motionEvent.getY();
      if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
        if (diffY > 0) {
          if (onSwipeDownListener != null) {
            onSwipeDownListener.onSwipeDown();
          }
        }
        result = true;
      }
    } catch (Exception exception) {
      Logger.e(LOG_TAG, "GestureListener: Error while handling fling gesture", exception);
    }
    return result;
  }
}
