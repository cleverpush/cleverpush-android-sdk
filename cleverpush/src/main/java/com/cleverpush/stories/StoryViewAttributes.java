package com.cleverpush.stories;

import android.graphics.Color;
import android.view.View;

public class StoryViewAttributes {
    public int backgroundColor = Color.WHITE;
    public int backgroundColorDarkMode = Color.WHITE;
    public int textColor = Color.BLACK;
    public int textColorDarkMode = Color.BLACK;
    public int storyViewHeight = -2; // wrap_content
    public int storyViewWidth = -1; // match_parent
    public String fontFamily = null;
    public String widgetId = null;
    public int titleVisibility = View.VISIBLE;
    public int titlePosition = 0; // position_default
    public int titleTextSize = 32;
    public int titleMinTextSize = 12;
    public int titleMaxTextSize = 32;
    public int storyIconHeight = 206;
    public int storyIconHeightPercentage = 0;
    public int storyIconWidth = 206;
    public float storyIconCornerRadius = -1;
    public float storyIconSpace = -1;
    public boolean storyIconShadow = false;
    public int borderVisibility = View.VISIBLE;
    public float borderMargin = 13.0F;
    public int borderWidth = 5;
    public int borderColor = Color.BLACK;
    public int borderColorDarkMode = Color.BLACK;
    public int borderColorLoading = Color.BLACK;
    public int borderColorLoadingDarkMode = Color.BLACK;
    public int subStoryUnreadCountVisibility = View.GONE;
    public int subStoryUnreadCountBackgroundColor = Color.BLACK;
    public int subStoryUnreadCountBackgroundColorDarkMode = Color.BLACK;
    public int subStoryUnreadCountTextColor = Color.WHITE;
    public int subStoryUnreadCountTextColorDarkMode = Color.WHITE;
    public int subStoryUnreadCountBadgeHeight = 78;
    public int subStoryUnreadCountBadgeWidth = 78;
    public int restrictToItems = 0;
    public int closeButtonPosition = 0; // left
    public int sortToLastIndex = 0; // position_default
    public boolean darkModeEnabled = false;
}
