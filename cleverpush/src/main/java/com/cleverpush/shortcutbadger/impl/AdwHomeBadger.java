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
public class AdwHomeBadger implements Badger {

  public static final String INTENT_UPDATE_COUNTER = "org.adw.launcher.counter.SEND";
  public static final String PACKAGENAME = "PNAME";
  public static final String CLASSNAME = "CNAME";
  public static final String COUNT = "COUNT";

  @Override
  public void executeBadge(Context context, ComponentName componentName, int badgeCount) throws ShortcutBadgeException {

    Intent intent = new Intent(INTENT_UPDATE_COUNTER);
    intent.putExtra(PACKAGENAME, componentName.getPackageName());
    intent.putExtra(CLASSNAME, componentName.getClassName());
    intent.putExtra(COUNT, badgeCount);
    if (BroadcastHelper.canResolveBroadcast(context, intent)) {
      context.sendBroadcast(intent);
    } else {
      throw new ShortcutBadgeException("AdwHomeBadger: unable to resolve intent: " + intent.toString());
    }
  }

  @Override
  public List<String> getSupportLaunchers() {
    return Arrays.asList(
        "org.adw.launcher",
        "org.adwfreak.launcher"
    );
  }
}
