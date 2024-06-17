package com.cleverpush.database;

import android.content.Context;

import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public class DatabaseClient {

  private Context mCtx;
  private static DatabaseClient mInstance;
  private CleverPushDatabase appDatabase;

  private DatabaseClient(Context mCtx) {
    this.mCtx = mCtx;
    appDatabase = Room.databaseBuilder(mCtx, CleverPushDatabase.class, "CleverPushDatabase")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .addMigrations(MIGRATION_1_2)
            .build();
  }

  public static synchronized DatabaseClient getInstance(Context mCtx) {
    if (mInstance == null) {
      mInstance = new DatabaseClient(mCtx);
    }
    return mInstance;
  }

  public CleverPushDatabase getAppDatabase() {
    return appDatabase;
  }

  Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(SupportSQLiteDatabase database) {
      // add column in TableBannerTrackEvent
      database.execSQL("ALTER TABLE cleverpush_tracked_events "
          + " ADD COLUMN eventPropertyRelation TEXT");
      database.execSQL("ALTER TABLE cleverpush_tracked_events "
          + " ADD COLUMN eventProperty TEXT");
      database.execSQL("ALTER TABLE cleverpush_tracked_events "
          + " ADD COLUMN eventPropertyValue TEXT");
    }
  };

}
