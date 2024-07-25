package com.cleverpush.util;

import android.content.Context;
import android.content.res.Configuration;

public class DarkModeHelper {

  public static boolean isDarkModeEnabled(Context context) {
    int currentNightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
    switch (currentNightMode) {
      case Configuration.UI_MODE_NIGHT_YES:
        return true;
      case Configuration.UI_MODE_NIGHT_NO:
      case Configuration.UI_MODE_NIGHT_UNDEFINED:
      default:
        return false;
    }
  }
}
