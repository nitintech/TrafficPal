package com.example.android.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by niarora on 11/3/2015.
 */
public class TrafficDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "trafficStorage.db";
    private static final int DATABASE_VERSION = 5;
    public TrafficDatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TrafficPathsDB.onCreate(db);
        NotificationsSDDB.onCreate(db);
        NotificationsDSDB.onCreate(db);
        LoggingDB.onCreate(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        //TrafficPathsDB.onUpgrade(db, oldVersion, newVersion);
        //NotificationsSDDB.onUpgrade(db, oldVersion, newVersion);
        //NotificationsDSDB.onUpgrade(db, oldVersion, newVersion);
        LoggingDB.onUpgrade(db, oldVersion, newVersion);
    }
}
