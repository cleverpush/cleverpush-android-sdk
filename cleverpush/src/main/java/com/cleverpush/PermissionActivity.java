package com.cleverpush;

import static com.cleverpush.Constants.LOG_TAG;

import android.Manifest;
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
    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String TAG = PermissionActivity.class.getCanonicalName();

    private static final HashMap<String, PermissionCallback> callbackMap = new HashMap<>();

    private String permissionType;

    public static void registerAsCallback(
            @NonNull String permissionType,
            @NonNull PermissionCallback callback
    ) {
        callbackMap.put(permissionType, callback);
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
        permissionType = extras.getString(INTENT_EXTRA_PERMISSION_TYPE);
        ActivityCompat.requestPermissions(this, new String[]{permissionType}, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean isGranted = ContextCompat.checkSelfPermission(this, permissionType) == PackageManager.PERMISSION_GRANTED;
            Logger.d(LOG_TAG, "onRequestPermissionsResult: " + permissions.length + " " + grantResults.length);

            PermissionCallback callback = callbackMap.get(permissionType);
            if (callback == null) {
                throw new RuntimeException("Missing callback for permissionType: " + permissionType);
            }

            if (isGranted) {
                Logger.d(LOG_TAG, "Permission granted: " + permissionType);
                callback.onGrant();
            } else {
                Logger.d(LOG_TAG, "Permission denied: " + permissionType);
                callback.onDeny();
            }
        }
        finish();
    }
}
