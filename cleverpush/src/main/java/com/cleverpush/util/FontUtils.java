package com.cleverpush.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import androidx.core.content.res.ResourcesCompat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FontUtils {
  private static final Map<String, Typeface> fontCache = new HashMap<>();

  @SuppressLint("DiscouragedApi")
  private static int getResourceId(Context context, String folder, String name) {
    return context.getResources().getIdentifier(name, folder, context.getPackageName());
  }

  private static Typeface findFontFromAssets(Context context, String name) {
    try {
      return Typeface.createFromAsset(context.getAssets(), name + ".ttf");
    } catch (Exception ignored) {

    }
    try {
      return Typeface.createFromAsset(context.getAssets(), "font/" + name + ".ttf");
    } catch (Exception ignored) {

    }
    try {
      return Typeface.createFromAsset(context.getAssets(), "fonts/" + name + ".ttf");
    } catch (Exception ignored) {

    }

    return null;
  }

  private static Typeface findFontFromResources(Context context, String name) {
    int resourceId = getResourceId(context, "font", name.toLowerCase(Locale.ROOT));
    if (resourceId == 0) {
      resourceId = getResourceId(context, "fonts", name.toLowerCase(Locale.ROOT));
    }
    if (resourceId != 0) {
      return ResourcesCompat.getFont(context, resourceId);
    }

    return null;
  }

  public static Typeface findFont(Context context, String name) {
    if (fontCache.containsKey(name)) {
      return fontCache.get(name);
    }

    Typeface font = findFontFromResources(context.getApplicationContext(), name);
    if (font == null) {
      font = findFontFromAssets(context.getApplicationContext(), name);
    }

    if (font != null) {
      fontCache.put(name, font);
    }

    return font;
  }
}
