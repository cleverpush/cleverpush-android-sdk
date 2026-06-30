package com.cleverpush.beacon;

import static com.cleverpush.Constants.LOG_TAG;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.cleverpush.CleverPush;
import com.cleverpush.listener.BeaconDetectedListener;
import com.cleverpush.util.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BeaconManager {

  private static final long SCAN_WINDOW_MS = 12_000L;
  private static final long FOREGROUND_IDLE_MS = 60_000L;
  private static final long BACKGROUND_IDLE_MS = 5 * 60_000L;

  private static final long FIRE_ONCE_COOLDOWN_MS = Long.MAX_VALUE;

  private static volatile BeaconManager instance;

  private final Context appContext;
  private final Handler handler = new Handler(Looper.getMainLooper());

  private final List<BeaconConfig> configBeacons = new ArrayList<>();
  private final Map<String, Long> lastTriggeredAt = new ConcurrentHashMap<>();

  private volatile BeaconDetectedListener listener;
  private long cooldownMs = FIRE_ONCE_COOLDOWN_MS;

  private BluetoothLeScanner scanner;
  private ScanCallback scanCallback;

  private boolean started = false;
  private boolean scanning = false;
  private boolean foreground = true;
  private boolean lifecycleRegistered = false;
  private boolean debugScanAllBeacons = false;
  private boolean foregroundTransitionState = false;
  private boolean retriggerOnForeground = true;
  private boolean monitoringRequested = false;
  private boolean bluetoothReceiverRegistered = false;
  private BroadcastReceiver bluetoothStateReceiver;

  private BeaconManager(Context context) {
    this.appContext = context.getApplicationContext();
  }

  public static BeaconManager getInstance(Context context) {
    if (instance == null) {
      synchronized (BeaconManager.class) {
        if (instance == null) {
          instance = new BeaconManager(context);
        }
      }
    }
    return instance;
  }

  private static String bytesToHex(byte[] bytes) {
    if (bytes == null) {
      return "null";
    }
    StringBuilder sb = new StringBuilder();
    for (byte b : bytes) {
      sb.append(String.format("%02x", b & 0xFF));
    }
    return sb.toString();
  }

  public void setBeaconDetectedListener(BeaconDetectedListener listener) {
    this.listener = listener;
  }

  public void setCooldownMs(long cooldownMs) {
    if (cooldownMs >= 0) {
      this.cooldownMs = cooldownMs;
      lastTriggeredAt.clear();
    }
  }

  /**
   * Enables debug BLE scanning for all beacon advertisements.
   * For diagnostics only; increases battery usage.
   * Do not enable in production. Call before {@link #start}.
   */
  public void setDebugScanAllBeacons(boolean enabled) {
    this.debugScanAllBeacons = enabled;
  }

  public boolean isStarted() {
    return started;
  }

  /**
   * Starts beacon monitoring for configured beacons.
   * Safe to call multiple times; later calls only refresh the beacon list.
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public synchronized void start(List<BeaconConfig> beacons) {
    try {
      configBeacons.clear();
      if (beacons != null) {
        configBeacons.addAll(beacons);
      }

      logDiagnostics();

      if (configBeacons.isEmpty()) {
        Logger.d(LOG_TAG, "BeaconManager: no beacons configured in channel config, skipping start.");
        if (started) {
          stop();
        }
        return;
      }

      if (!isBleSupported()) {
        Logger.w(LOG_TAG, "BeaconManager: BLE not supported on this device.");
        return;
      }

      monitoringRequested = true;
      registerLifecycleCallbacks();
      registerBluetoothStateReceiver();

      beginScanningIfPossible();
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: error during start.", t);
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private synchronized void beginScanningIfPossible() {
    if (!monitoringRequested || configBeacons.isEmpty()) {
      return;
    }

    BluetoothAdapter adapter = getBluetoothAdapter();
    if (adapter == null || !adapter.isEnabled()) {
      Logger.w(LOG_TAG, "BeaconManager: Bluetooth is disabled. Monitoring will start automatically once Bluetooth is enabled.");
      return;
    }

    scanner = adapter.getBluetoothLeScanner();
    if (scanner == null) {
      Logger.w(LOG_TAG, "BeaconManager: BluetoothLeScanner unavailable.");
      return;
    }

    if (isStarted()) {
      Logger.d(LOG_TAG, "BeaconManager: already started, refreshed " + configBeacons.size() + " beacon(s).");
      return;
    }

    started = true;
    Logger.d(LOG_TAG, "BeaconManager: started monitoring " + configBeacons.size() + " beacon(s).");
    scheduleScanCycle(0);
  }

  public synchronized void stop() {
    started = false;
    monitoringRequested = false;
    handler.removeCallbacksAndMessages(null);
    stopScanInternal();
    unregisterBluetoothStateReceiver();
    lastTriggeredAt.clear();
    Logger.d(LOG_TAG, "BeaconManager: stopped monitoring.");
  }

  private void registerBluetoothStateReceiver() {
    if (bluetoothReceiverRegistered) {
      return;
    }
    try {
      bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
          if (intent == null || !BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
            return;
          }
          if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return;
          }
          int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
          if (state == BluetoothAdapter.STATE_ON) {
            onBluetoothTurnedOn();
          } else if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_TURNING_OFF) {
            onBluetoothTurnedOff();
          }
        }
      };
      IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
      appContext.registerReceiver(bluetoothStateReceiver, filter);
      bluetoothReceiverRegistered = true;
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: failed to register Bluetooth state receiver.", t);
    }
  }

  private void unregisterBluetoothStateReceiver() {
    if (!bluetoothReceiverRegistered) {
      return;
    }
    try {
      appContext.unregisterReceiver(bluetoothStateReceiver);
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: failed to unregister Bluetooth state receiver.", t);
    } finally {
      bluetoothReceiverRegistered = false;
      bluetoothStateReceiver = null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private synchronized void onBluetoothTurnedOn() {
    if (!monitoringRequested || isStarted()) {
      return;
    }
    Logger.d(LOG_TAG, "BeaconManager: Bluetooth enabled - resuming beacon monitoring.");
    beginScanningIfPossible();
  }

  private synchronized void onBluetoothTurnedOff() {
    if (!isStarted()) {
      return;
    }
    Logger.d(LOG_TAG, "BeaconManager: Bluetooth disabled - pausing beacon scanning until it is re-enabled.");
    started = false;
    handler.removeCallbacksAndMessages(null);
    stopScanInternal();
  }

  private void scheduleScanCycle(long delayMs) {
    if (!isStarted()) {
      return;
    }
    handler.postDelayed(this::runScanWindow, delayMs);
  }

  @SuppressWarnings("MissingPermission")
  private void runScanWindow() {
    if (!isStarted()) {
      return;
    }
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
      return;
    }

    foreground = isAppInForeground();
    try {
      BluetoothAdapter adapter = getBluetoothAdapter();
      if (adapter == null || !adapter.isEnabled()) {
        // Bluetooth turned off mid-session: retry later instead of looping tightly.
        scheduleScanCycle(currentIdleMs());
        return;
      }
      if (scanner == null) {
        scanner = adapter.getBluetoothLeScanner();
      }
      if (scanner == null) {
        scheduleScanCycle(currentIdleMs());
        return;
      }

      startScanInternal();

      handler.postDelayed(() -> {
        stopScanInternal();
        scheduleScanCycle(currentIdleMs());
      }, currentScanWindowMs());
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: error running scan window.", t);
      scheduleScanCycle(currentIdleMs());
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  @SuppressWarnings("MissingPermission")
  private void startScanInternal() {
    if (scanning || scanner == null) {
      return;
    }
    try {
      if (scanCallback == null) {
        scanCallback = createScanCallback();
      }
      List<ScanFilter> filters = buildScanFilters();
      if (filters.isEmpty()) {
        filters = null;
      }
      ScanSettings settings = buildScanSettings();
      scanner.startScan(filters, settings, scanCallback);
      scanning = true;
      if (debugScanAllBeacons) {
        Logger.d(LOG_TAG, "BeaconManager: scan window started (foreground=" + foreground
                + ", filters=" + (filters == null ? "none/all" : String.valueOf(filters.size())) + ").");
      }
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: failed to start scan.", t);
    }
  }

  @SuppressWarnings("MissingPermission")
  private void stopScanInternal() {
    if (!scanning || scanner == null || scanCallback == null) {
      scanning = false;
      return;
    }
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        scanner.stopScan(scanCallback);
      }
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: failed to stop scan.", t);
    } finally {
      scanning = false;
    }
  }

  private long currentIdleMs() {
    if (debugScanAllBeacons) {
      return 3_000L;
    }
    return foreground ? FOREGROUND_IDLE_MS : BACKGROUND_IDLE_MS;
  }

  private long currentScanWindowMs() {
    return debugScanAllBeacons ? 9_000L : SCAN_WINDOW_MS;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanSettings buildScanSettings() {
    int scanMode;
    if (debugScanAllBeacons) {
      scanMode = ScanSettings.SCAN_MODE_LOW_LATENCY;
    } else {
      scanMode = foreground ? ScanSettings.SCAN_MODE_BALANCED : ScanSettings.SCAN_MODE_LOW_POWER;
    }
    ScanSettings.Builder builder = new ScanSettings.Builder().setScanMode(scanMode);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
              .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
              .setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT);
    }
    return builder.build();
  }

  /**
   * Builds BLE scan filters to limit scanning to relevant beacon advertisements,
   * improving battery efficiency. Uses hardware filters when possible and
   * supports debug mode for unfiltered scanning.
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private List<ScanFilter> buildScanFilters() {
    List<ScanFilter> filters = new ArrayList<>();

    if (debugScanAllBeacons) {
      return filters; // empty -> startScanInternal converts to null (unfiltered)
    }

    ScanFilter appleIBeaconFilter = buildAppleManufacturerFilter();
    if (appleIBeaconFilter != null) {
      filters.add(appleIBeaconFilter);
    }

    ScanFilter eddystoneFilter = buildEddystoneFilter();
    if (eddystoneFilter != null) {
      filters.add(eddystoneFilter);
    }

    for (BeaconConfig beacon : configBeacons) {
      ScanFilter serviceFilter = buildServiceUuidFilter(beacon.getUuid());
      if (serviceFilter != null) {
        filters.add(serviceFilter);
      }
    }
    return filters;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanFilter buildEddystoneFilter() {
    try {
      return new ScanFilter.Builder()
              .setServiceUuid(ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB"))
              .build();
    } catch (Throwable t) {
      return null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanFilter buildAppleManufacturerFilter() {
    try {
      return new ScanFilter.Builder()
              .setManufacturerData(0x004C, null)
              .build();
    } catch (Throwable t) {
      return null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanFilter buildServiceUuidFilter(String uuidString) {
    try {
      UUID uuid = UUID.fromString(uuidString);
      return new ScanFilter.Builder()
              .setServiceUuid(new ParcelUuid(uuid))
              .build();
    } catch (Throwable t) {
      return null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private ScanCallback createScanCallback() {
    return new ScanCallback() {
      @Override
      public void onScanResult(int callbackType, ScanResult result) {
        handleScanResult(result);
      }

      @Override
      public void onBatchScanResults(List<ScanResult> results) {
        if (results == null) {
          return;
        }
        for (ScanResult result : results) {
          handleScanResult(result);
        }
      }

      @Override
      public void onScanFailed(int errorCode) {
        Logger.e(LOG_TAG, "BeaconManager: scan failed with error code " + errorCode);
        scanning = false;
      }
    };
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void handleScanResult(ScanResult result) {
    try {
      if (debugScanAllBeacons) {
        logRawScanResult(result);
      }
      DetectedBeacon detected = BeaconParser.parse(result);
      if (detected == null || detected.getUuid() == null) {
        return;
      }
      if (debugScanAllBeacons) {
        Logger.d(LOG_TAG, "BeaconManager: detected " + detected.getType() + " beacon uuid="
                + detected.getUuid() + " major=" + detected.getMajor() + " minor=" + detected.getMinor()
                + " rssi=" + detected.getRssi());
      }

      BeaconConfig matched = matchBeacon(detected.getUuid());
      if (matched == null) {
        if (debugScanAllBeacons) {
          Logger.d(LOG_TAG, "BeaconManager: uuid " + detected.getUuid()
                  + " does not match any configured beacon, ignoring.");
        }
        return;
      }
      if (isInCooldown(detected.getUuid())) {
        if (debugScanAllBeacons) {
          Logger.d(LOG_TAG, "BeaconManager: uuid " + detected.getUuid()
                  + " matched but is within cooldown window, ignoring.");
        }
        return;
      }
      markTriggered(detected.getUuid());
      onBeaconMatched(detected, matched);
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: error handling scan result.", t);
    }
  }

  /**
   * Logs raw BLE advertisement details for diagnostics and debugging.
   * Helps inspect received beacon data and verify scanner results.
   */
  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private void logRawScanResult(ScanResult result) {
    try {
      if (result == null) {
        return;
      }
      String address = result.getDevice() != null ? result.getDevice().getAddress() : "?";
      StringBuilder sb = new StringBuilder("BeaconManager RAW -> addr=").append(address)
              .append(", rssi=").append(result.getRssi());

      ScanRecord record = result.getScanRecord();
      if (record != null) {
        sb.append(", name=").append(record.getDeviceName());
        if (record.getServiceUuids() != null) {
          sb.append(", services=").append(record.getServiceUuids());
        }
        android.util.SparseArray<byte[]> mfg = record.getManufacturerSpecificData();
        if (mfg != null && mfg.size() > 0) {
          for (int i = 0; i < mfg.size(); i++) {
            int id = mfg.keyAt(i);
            sb.append(", mfgId=0x").append(Integer.toHexString(id))
                    .append("[").append(bytesToHex(mfg.valueAt(i))).append("]");
          }
        }
        sb.append(", raw=").append(bytesToHex(record.getBytes()));
      }
      Logger.d(LOG_TAG, sb.toString());
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: error logging raw scan result.", t);
    }
  }

  private BeaconConfig matchBeacon(String detectedUuid) {
    if (detectedUuid == null) {
      return null;
    }
    for (BeaconConfig beacon : configBeacons) {
      if (detectedUuid.equalsIgnoreCase(beacon.getUuid())) {
        return beacon;
      }
    }
    return null;
  }

  private boolean isInCooldown(String uuid) {
    Long last = lastTriggeredAt.get(uuid.toLowerCase());
    if (last == null) {
      return false;
    }
    return (System.currentTimeMillis() - last) < cooldownMs;
  }

  private void markTriggered(String uuid) {
    lastTriggeredAt.put(uuid.toLowerCase(), System.currentTimeMillis());
  }

  /**
   * Handles a matched beacon by tracking its event and notifying the registered listener.
   */
  private void onBeaconMatched(DetectedBeacon detected, BeaconConfig matched) {
    Logger.d(LOG_TAG, "BeaconManager: matched beacon '" + matched.getUuid() + "' -> trackEvent('"
            + matched.getEventName() + "').");
    try {
      CleverPush.getInstance(appContext).trackEvent(matched.getEventName());
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: failed to trackEvent for matched beacon.", t);
    }

    final BeaconDetectedListener currentListener = listener;
    if (currentListener != null) {
      handler.post(() -> {
        try {
          currentListener.onBeaconDetected(detected, matched);
        } catch (Throwable t) {
          Logger.e(LOG_TAG, "BeaconManager: error in onBeaconDetected callback.", t);
        }
      });
    }
  }

  /**
   * Returns whether the app process is currently in the foreground.
   * Defaults to true if the check fails.
   */
  private boolean isAppInForeground() {
    try {
      ActivityManager.RunningAppProcessInfo info = new ActivityManager.RunningAppProcessInfo();
      ActivityManager.getMyMemoryState(info);
      return info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
              || info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
    } catch (Throwable t) {
      return true;
    }
  }

  /**
   * Handles app foreground/background transitions and
   * triggers actions when returning to the foreground.
   */
  private synchronized void updateForegroundTransition(boolean nowForeground) {
    if (nowForeground && !foregroundTransitionState) {
      foregroundTransitionState = true;
      onEnterForeground();
    } else if (!nowForeground && foregroundTransitionState) {
      foregroundTransitionState = false;
    }
  }

  @SuppressWarnings("MissingPermission")
  private void onEnterForeground() {
    foreground = true;
    if (retriggerOnForeground && cooldownMs == FIRE_ONCE_COOLDOWN_MS) {
      lastTriggeredAt.clear();
    }
    if (!isStarted()) {
      return;
    }
    Logger.d(LOG_TAG, "BeaconManager: app entered foreground - re-arming beacon events and scanning now.");
    handler.removeCallbacksAndMessages(null);
    stopScanInternal();
    scheduleScanCycle(0);
  }

  private void registerLifecycleCallbacks() {
    if (lifecycleRegistered) {
      return;
    }
    if (!(appContext instanceof Application)) {
      return;
    }
    try {
      ((Application) appContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
        private int startedActivities = 0;

        @Override
        public void onActivityStarted(@NonNull Activity activity) {
          startedActivities++;
          foreground = true;
          updateForegroundTransition(startedActivities > 0);
        }

        @Override
        public void onActivityStopped(@NonNull Activity activity) {
          if (startedActivities > 0) {
            startedActivities--;
          }
          if (startedActivities == 0) {
            foreground = false;
          }
          updateForegroundTransition(startedActivities > 0);
        }

        @Override
        public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        }

        @Override
        public void onActivityResumed(@NonNull Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull Activity activity) {
        }
      });
      lifecycleRegistered = true;
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: failed to register lifecycle callbacks.", t);
    }
  }

  /**
   * Logs diagnostic information relevant to BLE detection and setup.
   * Useful for troubleshooting beacon scanning issues.
   */
  private void logDiagnostics() {
    try {
      StringBuilder sb = new StringBuilder("BeaconManager diagnostics -> ");
      sb.append("androidSdk=").append(Build.VERSION.SDK_INT);
      sb.append(", device=").append(Build.MANUFACTURER).append(" ").append(Build.MODEL);
      sb.append(", isEmulator=").append(isProbablyEmulator());
      sb.append(", bleSupported=").append(isBleSupported());
      sb.append(", bluetoothEnabled=").append(isBluetoothEnabled());
      sb.append(", locationServicesOn=").append(isLocationServicesEnabled());
      sb.append(", debugScanAll=").append(debugScanAllBeacons);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        sb.append(", BLUETOOTH_SCAN=").append(hasPermission("android.permission.BLUETOOTH_SCAN"));
        sb.append(", BLUETOOTH_CONNECT=").append(hasPermission("android.permission.BLUETOOTH_CONNECT"));
      } else {
        sb.append(", ACCESS_FINE_LOCATION=").append(hasPermission("android.permission.ACCESS_FINE_LOCATION"));
      }
      sb.append(", configuredBeacons=").append(configBeacons.size());
      Logger.d(LOG_TAG, sb.toString());
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: error logging diagnostics.", t);
    }
  }

  private boolean hasPermission(String permission) {
    try {
      return ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED;
    } catch (Throwable t) {
      return false;
    }
  }

  /**
   * Returns whether system location services are enabled.
   * Required for BLE scanning on Android 11 and below.
   */
  public boolean isLocationServicesEnabled() {
    try {
      LocationManager lm = (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
      if (lm == null) {
        return false;
      }
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        return lm.isLocationEnabled();
      }
      return lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
              || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    } catch (Throwable t) {
      return false;
    }
  }

  private boolean isProbablyEmulator() {
    String fingerprint = Build.FINGERPRINT == null ? "" : Build.FINGERPRINT;
    String model = Build.MODEL == null ? "" : Build.MODEL;
    String product = Build.PRODUCT == null ? "" : Build.PRODUCT;
    return fingerprint.startsWith("generic")
            || fingerprint.startsWith("unknown")
            || fingerprint.contains("emulator")
            || model.contains("Emulator")
            || model.contains("Android SDK built for")
            || product.contains("sdk")
            || product.contains("emulator")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu");
  }

  public boolean isBleSupported() {
    return appContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
  }

  public boolean isBluetoothEnabled() {
    BluetoothAdapter adapter = getBluetoothAdapter();
    return adapter != null && adapter.isEnabled();
  }

  private BluetoothAdapter getBluetoothAdapter() {
    try {
      BluetoothManager manager = (BluetoothManager) appContext.getSystemService(Context.BLUETOOTH_SERVICE);
      if (manager != null) {
        return manager.getAdapter();
      }
    } catch (Throwable t) {
      Logger.e(LOG_TAG, "BeaconManager: error getting BluetoothManager.", t);
    }
    return BluetoothAdapter.getDefaultAdapter();
  }
}
