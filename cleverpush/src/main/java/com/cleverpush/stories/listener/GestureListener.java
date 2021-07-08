package com.cleverpush.stories.listener;

import android.view.GestureDetector;
import android.view.MotionEvent;

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
    public boolean onFling(MotionEvent motionEvent, MotionEvent motionEventTwo, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = motionEventTwo.getY() - motionEvent.getY();
            if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    if (onSwipeDownListener != null) {
                        onSwipeDownListener.onSwipeDown();
                    }
                }
                result = true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }
}
