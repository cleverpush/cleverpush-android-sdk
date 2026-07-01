package com.cleverpush.beacon;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class BeaconConfig {

  private final String id;
  private final String name;
  private final String channelId;
  private final String eventName;
  private final String uuid;
  private final String major;
  private final String minor;
  private final String storeId;

  public BeaconConfig(String id, String name, String channelId, String eventName, String uuid,
                      String major, String minor, String storeId) {
    this.id = id;
    this.name = name;
    this.channelId = channelId;
    this.eventName = eventName;
    this.uuid = uuid == null ? null : uuid.trim().toLowerCase();
    this.major = major;
    this.minor = minor;
    this.storeId = storeId;
  }

  public static BeaconConfig fromJson(JSONObject json) {
    if (json == null) {
      return null;
    }
    String uuid = json.optString("uuid", null);
    if (uuid != null) {
      uuid = uuid.trim();
    }
    if (uuid == null || uuid.isEmpty()) {
      return null;
    }
    return new BeaconConfig(
            json.optString("_id", null),
            json.optString("name", null),
            json.optString("channel", null),
            json.optString("eventName", null),
            uuid,
            json.optString("major", null),
            json.optString("minor", null),
            json.optString("storeId", null)
    );
  }

  public static List<BeaconConfig> listFromConfig(JSONObject channelConfig) {
    List<BeaconConfig> result = new ArrayList<>();
    if (channelConfig == null) {
      return result;
    }
    org.json.JSONArray array = channelConfig.optJSONArray("beacons");
    if (array == null) {
      return result;
    }
    for (int i = 0; i < array.length(); i++) {
      JSONObject item = array.optJSONObject(i);
      BeaconConfig config = fromJson(item);
      if (config != null && config.getEventName() != null && !config.getEventName().isEmpty()) {
        result.add(config);
      }
    }
    return result;
  }

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getChannelId() {
    return channelId;
  }

  public String getEventName() {
    return eventName;
  }

  public String getUuid() {
    return uuid;
  }

  public String getMajor() {
    return major;
  }

  public String getMinor() {
    return minor;
  }

  public String getStoreId() {
    return storeId;
  }

  @Override
  public String toString() {
    return "BeaconConfig{name='" + name + "', eventName='" + eventName + "', uuid='" + uuid
            + "', storeId='" + storeId + "'}";
  }
}
