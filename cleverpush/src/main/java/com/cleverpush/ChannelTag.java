package com.cleverpush;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class ChannelTag implements Serializable {
  @SerializedName("_id")
  String id;
  @SerializedName("name")
  String name;
  @SerializedName("autoAssignPath")
  String autoAssignPath;
  @SerializedName("autoAssignFunction")
  String autoAssignFunction;
  @SerializedName("autoAssignSelector")
  String autoAssignSelector;
  @SerializedName("autoAssignSessions")
  int autoAssignSessions;
  @SerializedName("autoAssignVisits")
  int autoAssignVisits;
  @SerializedName("autoAssignDays")
  int autoAssignDays;
  @SerializedName("autoAssignSeconds")
  int autoAssignSeconds;
  @SerializedName("autoAssignSessionVisits")
  boolean autoAssignSessionVisits;

  public String getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getAutoAssignPath() {
    return autoAssignPath;
  }

  public String getAutoAssignFunction() {
    return autoAssignFunction;
  }

  public String getAutoAssignSelector() {
    return autoAssignSelector;
  }

  public int getAutoAssignVisits() {
    return autoAssignVisits;
  }

  public int getAutoAssignDays() {
    return autoAssignDays;
  }

  public int getAutoAssignSeconds() {
    return autoAssignSeconds;
  }

  public int getAutoAssignSessions() {
    return autoAssignSessions;
  }

  public boolean isAutoAssignSessionVisits() {
    return autoAssignSessionVisits;
  }

  public String toString() {
    return this.getId();
  }
}
