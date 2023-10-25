package com.cleverpush.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TableBannerTrackEventDao {

  @Query("SELECT * FROM TableBannerTrackEvent ORDER BY Id ASC")
  List<TableBannerTrackEvent> getAll();

  @Insert
  void insert(TableBannerTrackEvent userTbl);

  @Update
  void update(TableBannerTrackEvent bannerTrackEvent);

  @Query("SELECT * FROM TableBannerTrackEvent WHERE bannerId = :bannerId AND trackEventId = :trackEventId "
          + "AND property = :property AND value = :value AND relation = :relation AND fromValue = :fromValue AND toValue = :toValue")
  List<TableBannerTrackEvent> getBannerTrackEvent(String bannerId, String trackEventId, String property, String value, String relation, String fromValue, String toValue);

  @Query("SELECT * FROM TableBannerTrackEvent WHERE trackEventId = :trackEventId ")
  List<TableBannerTrackEvent> getBannerTrackEvent(String trackEventId);

  @Query("UPDATE TableBannerTrackEvent SET count = count + 1, updatedDateTime = :updatedDate WHERE trackEventId = :eventId")
  void updateCount(String eventId, String updatedDate);

  @Query("DELETE FROM TableBannerTrackEvent")
  void deleteAll();

  @Query("DELETE FROM TableBannerTrackEvent WHERE createdDateTime <= strftime('%Y-%m-%d %H:%M:%S', datetime('now', '-' || :retentionDays || ' days'))")
  void deleteDataBasedOnRetentionDays(int retentionDays);

}
