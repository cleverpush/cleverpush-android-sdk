package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.cleverpush.util.Logger;

import java.util.HashMap;

public class PermissionActivity extends Activity implements ActivityCompat.OnRequestPermissionsResultCallback {
  interface PermissionCallback {
    void onGrant();

    void onDeny();
  }

  public static final String INTENT_EXTRA_PERMISSION_TYPE = "PERMISSION_TYPE";
  public static final String INTENT_EXTRA_PERMISSION_TYPES = "PERMISSION_TYPES";
  private static final int PERMISSION_REQUEST_CODE = 100;
  private static final String TAG = PermissionActivity.class.getCanonicalName();

  private static final HashMap<String, PermissionCallback> callbackMap = new HashMap<>();

  private String[] permissionTypes;
  private String callbackKey;

  public static void registerAsCallback(
      @NonNull String permissionType,
      @NonNull PermissionCallback callback
  ) {
    callbackMap.put(permissionType, callback);
  }

  public static String keyFor(@NonNull String[] permissionTypes) {
    return android.text.TextUtils.join(",", permissionTypes);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestPermission(getIntent().getExtras());
  }

  @Override
  protected void onNewIntent(Intent intent) {
    super.onNewIntent(intent);
    requestPermission(intent.getExtras());
  }

  private void requestPermission(Bundle extras) {
    if (extras == null) {
      finish();
      return;
    }
    String[] types = extras.getStringArray(INTENT_EXTRA_PERMISSION_TYPES);
    if (types == null) {
      String single = extras.getString(INTENT_EXTRA_PERMISSION_TYPE);
      if (single == null) {
        finish();
        return;
      }
      types = new String[] {single};
    }
    permissionTypes = types;
    callbackKey = keyFor(types);
    ActivityCompat.requestPermissions(this, permissionTypes, PERMISSION_REQUEST_CODE);
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    if (requestCode == PERMISSION_REQUEST_CODE) {
      boolean allGranted = true;
      for (String permission : permissionTypes) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
          allGranted = false;
          break;
        }
      }
      Logger.d(LOG_TAG, "onRequestPermissionsResult: " + permissions.length + " " + grantResults.length);

      PermissionCallback callback = callbackMap.get(callbackKey);
      if (callback == null && permissionTypes.length == 1) {
        callback = callbackMap.get(permissionTypes[0]);
      }
      if (callback == null) {
        Logger.w(LOG_TAG, "onRequestPermissionsResult: Missing callback for permissionType: " + callbackKey);
        finish();
        return;
      }

      if (allGranted) {
        Logger.d(LOG_TAG, "Permission granted: " + callbackKey);
        callback.onGrant();
      } else {
        Logger.d(LOG_TAG, "Permission denied: " + callbackKey);
        callback.onDeny();
      }
    }
    finish();
  }
}
