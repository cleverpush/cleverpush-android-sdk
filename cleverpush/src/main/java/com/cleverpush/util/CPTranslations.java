package com.cleverpush.util;

import java.util.HashMap;
import java.util.Map;

public class CPTranslations {

  private static final Map<String, String> translations = new HashMap<>();

  static {
    translations.put("locationPermissionDialog.title.de", "Erlaubnis erforderlich");
    translations.put("locationPermissionDialog.message.de", "Diese App benötigt Hintergrundzugriff auf den Standort, um Geofencing-Funktionen zu aktivieren. Bitte aktivieren Sie diese Berechtigung in den App-Einstellungen.\n\nBerechtigung -> Standort -> Immer zulassen");
    translations.put("locationPermissionDialog.positiveButton.de", "Gehe zu den Einstellungen");
    translations.put("locationPermissionDialog.negativeButton.de", "Stornieren");

    translations.put("locationPermissionDialog.title.en", "Permission Required");
    translations.put("locationPermissionDialog.message.en", "This app needs background location access to enable geofencing features. Please enable this permission in the app settings.\n\nPermission -> Location -> Allow all the time");
    translations.put("locationPermissionDialog.positiveButton.en", "Go to Settings");
    translations.put("locationPermissionDialog.negativeButton.en", "Cancel");
  }

  public static String get(String raw) {
    if (translations.containsKey(raw)) {
      return translations.get(raw);
    } else {
      throw new IllegalArgumentException("Unknown CPTranslations: " + raw);
    }
  }
}
