package com.cleverpush.beacon;

public class DetectedBeacon {

  public static final String TYPE_IBEACON = "ibeacon";
  public static final String TYPE_ALTBEACON = "altbeacon";
  public static final String TYPE_EDDYSTONE = "eddystone";
  public static final String TYPE_GENERIC = "generic";
  private final String uuid;
  private final Integer major;
  private final Integer minor;
  private final int rssi;
  private final Integer txPower;
  private final String bluetoothAddress;
  private final String type;

  public DetectedBeacon(String uuid, Integer major, Integer minor, int rssi, Integer txPower,
                        String bluetoothAddress, String type) {
    this.uuid = uuid == null ? null : uuid.trim().toLowerCase();
    this.major = major;
    this.minor = minor;
    this.rssi = rssi;
    this.txPower = txPower;
    this.bluetoothAddress = bluetoothAddress;
    this.type = type;
  }

  public String getUuid() {
    return uuid;
  }

  public Integer getMajor() {
    return major;
  }

  public Integer getMinor() {
    return minor;
  }

  public int getRssi() {
    return rssi;
  }

  public Integer getTxPower() {
    return txPower;
  }

  public String getBluetoothAddress() {
    return bluetoothAddress;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "DetectedBeacon{uuid='" + uuid + "', major=" + major + ", minor=" + minor
            + ", rssi=" + rssi + ", txPower=" + txPower + ", type='" + type + "'}";
  }
}
