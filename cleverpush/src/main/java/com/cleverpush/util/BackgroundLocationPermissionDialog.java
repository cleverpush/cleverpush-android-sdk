package com.cleverpush.util;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;

import java.util.Locale;

/**
 * For geofencing, ACCESS_BACKGROUND_LOCATION permission is needed starting from Android 10 (API level 29).
 * This is because geofencing is a feature that typically requires app to receive location updates
 * even when it is not in the foreground, which qualifies as background location access.
 */
public class BackgroundLocationPermissionDialog {

  private static final String LOG_TAG = "LocationPermissionDialog";

  public static void show(Activity activity) {
    try {
      Locale currentLocale = Locale.getDefault();
      String title, message, positiveButton, negativeButton;

      if ("de".equals(currentLocale.getLanguage())) {
        title = CPTranslations.get("locationPermissionDialog.title.de");
        message = CPTranslations.get("locationPermissionDialog.message.de");
        positiveButton = CPTranslations.get("locationPermissionDialog.positiveButton.de");
        negativeButton = CPTranslations.get("locationPermissionDialog.negativeButton.de");
      } else {
        title = CPTranslations.get("locationPermissionDialog.title.en");
        message = CPTranslations.get("locationPermissionDialog.message.en");
        positiveButton = CPTranslations.get("locationPermissionDialog.positiveButton.en");
        negativeButton = CPTranslations.get("locationPermissionDialog.negativeButton.en");
      }

      new AlertDialog.Builder(activity)
          .setTitle(title)
          .setMessage(message)
          .setPositiveButton(positiveButton, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              requestBackgroundLocationPermission(activity);
            }
          })
          .setNegativeButton(negativeButton, null)
          .show();
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while displaying settings dialog. " + e.getLocalizedMessage(), e);
    }
  }

  private static void requestBackgroundLocationPermission(Activity activity) {
    try {
      Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
      intent.setData(uri);
      activity.startActivity(intent);
    } catch (Exception e) {
      Logger.e(LOG_TAG, "Error while opening application's detail setting screen. " + e.getLocalizedMessage(), e);
    }
  }
}
