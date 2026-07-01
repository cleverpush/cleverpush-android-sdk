package com.cleverpush.beacon;

import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.SparseArray;

import androidx.annotation.RequiresApi;

import java.util.List;
import java.util.Map;

public class BeaconParser {

  private static final int APPLE_MANUFACTURER_ID = 0x004C;
  private static final byte IBEACON_TYPE = 0x02;
  private static final byte IBEACON_LENGTH = 0x15; // 21 bytes following
  private static final int ALTBEACON_CODE = 0xBEAC;

  private static final ParcelUuid EDDYSTONE_SERVICE =
          ParcelUuid.fromString("0000FEAA-0000-1000-8000-00805F9B34FB");
  private static final byte EDDYSTONE_FRAME_UID = 0x00;

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  public static DetectedBeacon parse(ScanResult scanResult) {
    if (scanResult == null) {
      return null;
    }
    try {
      ScanRecord record = scanResult.getScanRecord();
      if (record == null) {
        return null;
      }

      String address = scanResult.getDevice() != null ? scanResult.getDevice().getAddress() : null;
      int rssi = scanResult.getRssi();

      DetectedBeacon beacon = parseIBeacon(record, rssi, address);
      if (beacon != null) {
        return beacon;
      }
      beacon = parseAltBeacon(record, rssi, address);
      if (beacon != null) {
        return beacon;
      }
      beacon = parseEddystone(record, rssi, address);
      if (beacon != null) {
        return beacon;
      }
      return parseGeneric(record, rssi, address);
    } catch (Throwable ignored) {
      // Never let a malformed packet crash the scan callback.
      return null;
    }
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static DetectedBeacon parseIBeacon(ScanRecord record, int rssi, String address) {
    byte[] data = record.getManufacturerSpecificData(APPLE_MANUFACTURER_ID);
    if (data == null || data.length < 23) {
      return null;
    }
    int offset = -1;
    if (data[0] == IBEACON_TYPE && data[1] == IBEACON_LENGTH) {
      offset = 0;
    } else {
      for (int i = 0; i + 23 <= data.length; i++) {
        if (data[i] == IBEACON_TYPE && data[i + 1] == IBEACON_LENGTH) {
          offset = i;
          break;
        }
      }
    }
    if (offset < 0 || offset + 23 > data.length) {
      return null;
    }

    String uuid = formatUuid(toHex(data, offset + 2, 16));
    int major = ((data[offset + 18] & 0xFF) << 8) | (data[offset + 19] & 0xFF);
    int minor = ((data[offset + 20] & 0xFF) << 8) | (data[offset + 21] & 0xFF);
    int txPower = data[offset + 22]; // signed

    return new DetectedBeacon(uuid, major, minor, rssi, txPower, address, DetectedBeacon.TYPE_IBEACON);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static DetectedBeacon parseAltBeacon(ScanRecord record, int rssi, String address) {
    SparseArray<byte[]> all = record.getManufacturerSpecificData();
    if (all == null) {
      return null;
    }
    for (int i = 0; i < all.size(); i++) {
      byte[] data = all.valueAt(i);
      if (data == null || data.length < 24) {
        continue;
      }
      int code = ((data[0] & 0xFF) << 8) | (data[1] & 0xFF);
      if (code != ALTBEACON_CODE) {
        continue;
      }
      String uuid = formatUuid(toHex(data, 2, 16));
      int id2 = ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
      int id3 = ((data[20] & 0xFF) << 8) | (data[21] & 0xFF);
      int txPower = data[22]; // signed reference RSSI
      return new DetectedBeacon(uuid, id2, id3, rssi, txPower, address, DetectedBeacon.TYPE_ALTBEACON);
    }
    return null;
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static DetectedBeacon parseEddystone(ScanRecord record, int rssi, String address) {
    Map<ParcelUuid, byte[]> serviceData = record.getServiceData();
    if (serviceData == null) {
      return null;
    }
    byte[] data = serviceData.get(EDDYSTONE_SERVICE);
    if (data == null || data.length < 18) {
      return null;
    }
    if (data[0] != EDDYSTONE_FRAME_UID) {
      return null; // not a UID frame (URL/TLM/EID frames carry no stable id we can match)
    }
    int txPower = data[1]; // signed calibrated tx power at 0m
    String uuid = formatUuid(toHex(data, 2, 16)); // namespace (10) + instance (6) = 16 bytes
    return new DetectedBeacon(uuid, null, null, rssi, txPower, address, DetectedBeacon.TYPE_EDDYSTONE);
  }

  @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
  private static DetectedBeacon parseGeneric(ScanRecord record, int rssi, String address) {
    List<ParcelUuid> serviceUuids = record.getServiceUuids();
    if (serviceUuids == null || serviceUuids.isEmpty()) {
      return null;
    }
    ParcelUuid first = serviceUuids.get(0);
    if (first == null) {
      return null;
    }
    String uuid = first.getUuid().toString();
    return new DetectedBeacon(uuid, null, null, rssi, null, address, DetectedBeacon.TYPE_GENERIC);
  }

  private static String toHex(byte[] data, int start, int length) {
    StringBuilder sb = new StringBuilder();
    for (int i = start; i < start + length && i < data.length; i++) {
      sb.append(String.format("%02x", data[i] & 0xFF));
    }
    return sb.toString();
  }

  /**
   * Turns a 32-char hex string into the canonical 8-4-4-4-12 UUID format.
   */
  private static String formatUuid(String hex) {
    if (hex == null || hex.length() != 32) {
      return hex;
    }
    return hex.substring(0, 8) + "-"
            + hex.substring(8, 12) + "-"
            + hex.substring(12, 16) + "-"
            + hex.substring(16, 20) + "-"
            + hex.substring(20, 32);
  }
}
