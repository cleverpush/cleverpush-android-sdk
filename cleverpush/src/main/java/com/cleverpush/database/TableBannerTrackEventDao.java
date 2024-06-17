package com.cleverpush.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TableBannerTrackEventDao {

  @Query("SELECT * FROM cleverpush_tracked_events ORDER BY Id ASC")
  List<TableBannerTrackEvent> getAll();

  @Insert
  void insert(TableBannerTrackEvent userTbl);

  @Update
  void update(TableBannerTrackEvent bannerTrackEvent);

  @Query("SELECT * FROM cleverpush_tracked_events WHERE bannerId = :bannerId AND eventId = :eventId "
      + "AND property = :property AND value = :value AND relation = :relation AND fromValue = :fromValue AND toValue = :toValue")
  List<TableBannerTrackEvent> getBannerTrackEvent(String bannerId, String eventId, String property, String value, String relation, String fromValue, String toValue);

  @Query("SELECT * FROM cleverpush_tracked_events WHERE bannerId = :bannerId AND eventId = :eventId "
      + "AND property = :property AND value = :value AND relation = :relation AND fromValue = :fromValue AND toValue = :toValue "
      + "AND eventPropertyRelation = :eventPropertyRelation AND eventProperty = :eventProperty AND eventPropertyValue = :eventPropertyValue")
  List<TableBannerTrackEvent> getBannerTrackEvent(String bannerId, String eventId, String property, String value, String relation, String fromValue, String toValue
      , String eventPropertyRelation, String eventProperty, String eventPropertyValue);

  @Query("SELECT * FROM cleverpush_tracked_events WHERE eventId = :eventId ")
  List<TableBannerTrackEvent> getBannerTrackEvent(String eventId);

  @Query("SELECT * FROM cleverpush_tracked_events WHERE eventId = :eventId AND eventProperty = :eventProperty AND eventPropertyValue = :eventPropertyValue")
  List<TableBannerTrackEvent> getBannerTrackEvent(String eventId, String eventProperty, String eventPropertyValue);

  @Query("UPDATE cleverpush_tracked_events SET count = count + 1, updatedDateTime = :updatedDate WHERE eventId = :eventId")
  void increaseCount(String eventId, String updatedDate);

  @Query("UPDATE cleverpush_tracked_events SET count = count + 1, updatedDateTime = :updatedDate WHERE eventId = :eventId AND eventProperty = :eventProperty AND eventPropertyValue = :eventPropertyValue")
  void increaseCount(String eventId, String updatedDate, String eventProperty, String eventPropertyValue);

  @Query("DELETE FROM cleverpush_tracked_events")
  void deleteAll();

  @Query("DELETE FROM cleverpush_tracked_events WHERE createdDateTime <= strftime('%Y-%m-%d %H:%M:%S', datetime('now', '-' || :retentionDays || ' days'))")
  void deleteDataBasedOnRetentionDays(int retentionDays);

}
