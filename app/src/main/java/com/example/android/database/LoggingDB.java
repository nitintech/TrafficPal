package com.example.android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by niarora on 7/21/2016.
 */
public class LoggingDB {

    public static final String TABLE_LOGGING = "Logging_Table";
    public static final String COLUMN_ID = "_id";
    public static final String DIRECTION = "direction";
    public static final String DATE = "Date";
    public static final String DAY = "Day";
    public static final String TIME = "time";
    public static final String DURATION = "LoggedDuration";


    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_LOGGING
            + "("
            + COLUMN_ID + " integer not null, "        //0
            + DIRECTION + " integer not null, "        //1
            + DATE + " integer, "                      //2: save as unix time .. Use System.currentTimeMillis()
            + TIME + " integer, "                      //3
            + DAY + " text, "                          //4
            + DURATION + " integer"                    //5
            + ");";

    public static void onCreate(SQLiteDatabase database){
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(TrafficPathsDB.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_LOGGING);
        onCreate(database);
    }
}
