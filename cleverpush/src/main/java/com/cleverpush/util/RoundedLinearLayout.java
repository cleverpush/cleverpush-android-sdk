package com.cleverpush.util;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class RoundedLinearLayout extends LinearLayout {
  private float cornerRadius;

  public RoundedLinearLayout(Context context) {
    super(context);
  }

  public RoundedLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public RoundedLinearLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  public void setCornerRadius(float cornerRadius) {
    this.cornerRadius = cornerRadius;
    invalidate();
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    Path path = new Path();
    path.addRoundRect(new RectF(0, 0, getWidth(), getHeight()), cornerRadius, cornerRadius, Path.Direction.CW);
    canvas.clipPath(path);
    super.dispatchDraw(canvas);
  }
}
