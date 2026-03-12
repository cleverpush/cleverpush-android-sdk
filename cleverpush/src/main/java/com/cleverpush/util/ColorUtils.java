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
    // Android expects #AARRGGBB (alpha first). If we have #RRGGBBAA (alpha last, e.g. from CSS/web),
    // convert it. Only convert when Android would see alpha=00 (first 2 digits), so we don't break
    // Android-style #AARRGGBB with non-zero alpha.
    if (colorStr != null && colorStr.length() == 9 && colorStr.charAt(0) == '#'
        && colorStr.substring(1, 3).equals("00")) {
      String rr = colorStr.substring(1, 3);
      String gg = colorStr.substring(3, 5);
      String bb = colorStr.substring(5, 7);
      String aa = colorStr.substring(7, 9);
      colorStr = "#" + aa + rr + gg + bb;
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
