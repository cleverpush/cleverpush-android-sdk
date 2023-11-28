package com.cleverpush.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "cleverpush_tracked_events")
public class TableBannerTrackEvent {

  @PrimaryKey(autoGenerate = true)
  private int id;
  @ColumnInfo(name = "bannerId")
  private String bannerId;
  @ColumnInfo(name = "eventId")
  private String eventId;
  @ColumnInfo(name = "relation")
  private String relation;
  @ColumnInfo(name = "property")
  private String property;
  @ColumnInfo(name = "value")
  private String value;
  @ColumnInfo(name = "fromValue")
  private String fromValue;
  @ColumnInfo(name = "toValue")
  private String toValue;
  @ColumnInfo(name = "count")
  private int count;
  @ColumnInfo(name = "createdDateTime")
  private String createdDateTime;
  @ColumnInfo(name = "updatedDateTime")
  private String updatedDateTime;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getBannerId() {
    return bannerId;
  }

  public void setBannerId(String bannerId) {
    this.bannerId = bannerId;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getRelation() {
    return relation;
  }

  public void setRelation(String relation) {
    this.relation = relation;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getFromValue() {
    return fromValue;
  }

  public void setFromValue(String fromValue) {
    this.fromValue = fromValue;
  }

  public String getToValue() {
    return toValue;
  }

  public void setToValue(String toValue) {
    this.toValue = toValue;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getCreatedDateTime() {
    return createdDateTime;
  }

  public void setCreatedDateTime(String createdDateTime) {
    this.createdDateTime = createdDateTime;
  }

  public String getUpdatedDateTime() {
    return updatedDateTime;
  }

  public void setUpdatedDateTime(String updatedDateTime) {
    this.updatedDateTime = updatedDateTime;
  }
}
