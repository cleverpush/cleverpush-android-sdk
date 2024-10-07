package com.cleverpush.util;

import static com.cleverpush.Constants.LOG_TAG;

import android.graphics.Color;

public class ColorUtils {
  public static int parseColor(String colorStr) {
    if (colorStr != null) {
      colorStr = colorStr.trim();
    }
    if (colorStr != null && colorStr.length() == 4 && colorStr.charAt(0) == '#') {
      colorStr =
          "#" + colorStr.charAt(1) + colorStr.charAt(1) + colorStr.charAt(2) + colorStr.charAt(2) + colorStr.charAt(3)
              + colorStr.charAt(3);
    }
    int color = Color.BLACK;
    try {
      color = Color.parseColor(colorStr);
    } catch (Exception ex) {
      Logger.e(LOG_TAG, "Error parsing color: " + colorStr, ex);
    }
    return color;
  }
}
