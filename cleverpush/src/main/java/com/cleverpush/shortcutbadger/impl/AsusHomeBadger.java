// Subpackaged to prevent conflicts with other plugins

package com.cleverpush.shortcutbadger.impl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Arrays;
import java.util.List;

import com.cleverpush.shortcutbadger.Badger;
import com.cleverpush.shortcutbadger.ShortcutBadgeException;
import com.cleverpush.shortcutbadger.util.BroadcastHelper;

/**
 * @author leolin
 */
public class AsusHomeBadger implements Badger {

  private static final String INTENT_ACTION = "android.intent.action.BADGE_COUNT_UPDATE";
  private static final String INTENT_EXTRA_BADGE_COUNT = "badge_count";
  private static final String INTENT_EXTRA_PACKAGENAME = "badge_count_package_name";
  private static final String INTENT_EXTRA_ACTIVITY_NAME = "badge_count_class_name";

  @Override
  public void executeBadge(Context context, ComponentName componentName, int badgeCount) throws ShortcutBadgeException {
    Intent intent = new Intent(INTENT_ACTION);
    intent.putExtra(INTENT_EXTRA_BADGE_COUNT, badgeCount);
    intent.putExtra(INTENT_EXTRA_PACKAGENAME, componentName.getPackageName());
    intent.putExtra(INTENT_EXTRA_ACTIVITY_NAME, componentName.getClassName());
    intent.putExtra("badge_vip_count", 0);
    if (BroadcastHelper.canResolveBroadcast(context, intent)) {
      context.sendBroadcast(intent);
    } else {
      throw new ShortcutBadgeException("AsusHomeBadger: unable to resolve intent: " + intent.toString());
    }
  }

  @Override
  public List<String> getSupportLaunchers() {
    return Arrays.asList("com.asus.launcher");
  }
}
