package com.example.android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by niarora on 11/3/2015.
 * Table for storing the traffic routes
 */
public class TrafficPathsDB {
    public static final String TABLE_TRAFFICPATHS = "trafficpaths";
    public static final String COLUMN_ID = "_id";
    public static final String SOURCE_ADDRESS = "sourceaddress";
    public static final String DEST_ADDRESS = "destaddress";
    public static final String SOURCE_LATLNG = "sourcelatlng";
    public static final String DEST_LATLNG = "destlatlng";
    public static final String NOTIFICATIONS_ENABLED = "notifications";
    public static final String LOGGING_ENABLED = "logging";


    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_TRAFFICPATHS
            + "("
            + COLUMN_ID + " integer primary key autoincrement, " //0
            + SOURCE_ADDRESS + " text not null, "                //1
            + DEST_ADDRESS + " text not null,"                   //2
            + SOURCE_LATLNG + " text not null,"                  //3
            + DEST_LATLNG + " text not null,"                    //4
            + NOTIFICATIONS_ENABLED + " integer not null,"       //5
            +LOGGING_ENABLED + " integer not null"               //6
            + ");";

    public static void onCreate(SQLiteDatabase database){
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(TrafficPathsDB.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_TRAFFICPATHS);
        onCreate(database);
    }
}
