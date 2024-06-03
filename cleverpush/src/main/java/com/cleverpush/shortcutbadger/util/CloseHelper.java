// Subpackaged to prevent conflicts with other plugins

package com.cleverpush.shortcutbadger.util;

import static com.cleverpush.Constants.LOG_TAG;

import android.database.Cursor;

import com.cleverpush.util.Logger;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author leolin
 */
public class CloseHelper {

  public static void close(Cursor cursor) {
    try {
      if (cursor != null && !cursor.isClosed()) {
        cursor.close();
      }
    } catch (Exception e) {
      Logger.e(LOG_TAG, "CloseHelper: Error while closing cursor", e);
    }
  }


  public static void closeQuietly(Closeable closeable) {
    try {
      if (closeable != null) {
        closeable.close();
      }
    } catch (IOException e) {
      Logger.e(LOG_TAG, "CloseHelper: Error while closing closeable", e);
    }
  }
}
