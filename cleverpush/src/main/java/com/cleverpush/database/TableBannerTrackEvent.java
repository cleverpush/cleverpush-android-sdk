package com.cleverpush.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "TableBannerTrackEvent")
public class TableBannerTrackEvent {

  @PrimaryKey(autoGenerate = true)
  private int id;
  @ColumnInfo(name = "banner_id")
  private String banner_id;
  @ColumnInfo(name = "track_event_id")
  private String track_event_id;
  @ColumnInfo(name = "relation")
  private String relation;
  @ColumnInfo(name = "property")
  private String property;
  @ColumnInfo(name = "value")
  private String value;
  @ColumnInfo(name = "from_value")
  private String from_value;
  @ColumnInfo(name = "to_value")
  private String to_value;
  @ColumnInfo(name = "count")
  private int count;
  @ColumnInfo(name = "created_date_time")
  private String created_date_time;
  @ColumnInfo(name = "updated_date_time")
  private String updated_date_time;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getBanner_id() {
    return banner_id;
  }

  public void setBanner_id(String banner_id) {
    this.banner_id = banner_id;
  }

  public String getTrack_event_id() {
    return track_event_id;
  }

  public void setTrack_event_id(String track_event_id) {
    this.track_event_id = track_event_id;
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

  public String getFrom_value() {
    return from_value;
  }

  public void setFrom_value(String from_value) {
    this.from_value = from_value;
  }

  public String getTo_value() {
    return to_value;
  }

  public void setTo_value(String to_value) {
    this.to_value = to_value;
  }

  public int getCount() {
    return count;
  }

  public void setCount(int count) {
    this.count = count;
  }

  public String getCreated_date_time() {
    return created_date_time;
  }

  public void setCreated_date_time(String created_date_time) {
    this.created_date_time = created_date_time;
  }

  public String getUpdated_date_time() {
    return updated_date_time;
  }

  public void setUpdated_date_time(String updated_date_time) {
    this.updated_date_time = updated_date_time;
  }
}
