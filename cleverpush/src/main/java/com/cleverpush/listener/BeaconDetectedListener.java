package com.cleverpush.listener;

import com.cleverpush.beacon.BeaconConfig;
import com.cleverpush.beacon.DetectedBeacon;

/**
 * Optional callback that is invoked only AFTER a scanned BLE/iBeacon advertisement has been
 * successfully matched against a beacon configured in the CleverPush channel config.
 *
 * The callback receives both the raw detected beacon (UUID/major/minor/rssi) and the matched
 * configuration object (name/eventName/storeId), so the integrating app can react if it wants to.
 * Registering this callback is completely optional - the SDK already tracks the configured event
 * automatically before the callback fires.
 */
public interface BeaconDetectedListener {
  void onBeaconDetected(DetectedBeacon detectedBeacon, BeaconConfig matchedBeacon);
}
