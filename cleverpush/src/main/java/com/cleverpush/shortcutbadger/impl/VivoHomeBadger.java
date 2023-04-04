// Subpackaged to prevent conflicts with other plugins

package com.cleverpush.shortcutbadger.impl;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import java.util.Arrays;
import java.util.List;

import com.cleverpush.shortcutbadger.Badger;
import com.cleverpush.shortcutbadger.ShortcutBadgeException;

/**
 * @author leolin
 */
public class VivoHomeBadger implements Badger {

  @Override
  public void executeBadge(Context context, ComponentName componentName, int badgeCount) throws ShortcutBadgeException {
    Intent intent = new Intent("launcher.action.CHANGE_APPLICATION_NOTIFICATION_NUM");
    intent.putExtra("packageName", context.getPackageName());
    intent.putExtra("className", componentName.getClassName());
    intent.putExtra("notificationNum", badgeCount);
    context.sendBroadcast(intent);
  }

  @Override
  public List<String> getSupportLaunchers() {
    return Arrays.asList("com.vivo.launcher");
  }
}
