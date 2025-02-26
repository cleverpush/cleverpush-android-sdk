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
 * @author Gernot Pansy
 */
public class ApexHomeBadger implements Badger {

  private static final String INTENT_UPDATE_COUNTER = "com.anddoes.launcher.COUNTER_CHANGED";
  private static final String PACKAGENAME = "package";
  private static final String COUNT = "count";
  private static final String CLASS = "class";

  @Override
  public void executeBadge(Context context, ComponentName componentName, int badgeCount) throws ShortcutBadgeException {

    Intent intent = new Intent(INTENT_UPDATE_COUNTER);
    intent.putExtra(PACKAGENAME, componentName.getPackageName());
    intent.putExtra(COUNT, badgeCount);
    intent.putExtra(CLASS, componentName.getClassName());
    if (BroadcastHelper.canResolveBroadcast(context, intent)) {
      context.sendBroadcast(intent);
    } else {
      throw new ShortcutBadgeException("ApexHomeBadger: unable to resolve intent: " + intent.toString());
    }
  }

  @Override
  public List<String> getSupportLaunchers() {
    return Arrays.asList("com.anddoes.launcher");
  }
}
