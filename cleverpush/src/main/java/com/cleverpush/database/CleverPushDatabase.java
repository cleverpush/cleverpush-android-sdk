package com.cleverpush.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {TableBannerTrackEvent.class}, version = 1)
public abstract class CleverPushDatabase extends RoomDatabase {

  public abstract TableBannerTrackEventDao trackEventDao();
}
