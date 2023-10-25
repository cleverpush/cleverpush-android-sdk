package com.cleverpush.database;

import android.content.Context;

import androidx.room.Room;

public class DatabaseClient {

  private Context mCtx;
  private static DatabaseClient mInstance;
  private CleverPushDatabase appDatabase;

  private DatabaseClient(Context mCtx) {
    this.mCtx = mCtx;
    appDatabase = Room.databaseBuilder(mCtx, CleverPushDatabase.class, "CleverPushDatabase")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .addMigrations()
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
}
