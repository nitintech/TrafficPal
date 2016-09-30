package com.example.android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by niarora on 11/7/2015.
 */
public class NotificationsSDDB {

    public static final String TABLE_NOTIFICATIONS_SD = "Notifications_table_SD";
    public static final String COLUMN_ID = "_id";
    public static final String MONDAY_SD = "mondaysd";
    public static final String TUESDAY_SD = "tuesdaysd";
    public static final String WEDNESDAY_SD = "wednesdaysd";
    public static final String THURSDAY_SD = "thursdaysd";
    public static final String FRIDAY_SD = "fridaysd";
    public static final String SATURDAY_SD = "saturdaysd";
    public static final String SUNDAY_SD = "sundaysd";


    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_NOTIFICATIONS_SD
            + "("
            + COLUMN_ID + " integer not null, " //0
            + MONDAY_SD + " integer, "                //1
            + TUESDAY_SD + " integer ,"                   //2
            + WEDNESDAY_SD + " integer, "                  //3
            + THURSDAY_SD + " integer, "                    //4
            + FRIDAY_SD + " integer, "       //5
            + SATURDAY_SD + " integer, "               //6
            + SUNDAY_SD + " integer"               //7
            + ");";

    public static void onCreate(SQLiteDatabase database){
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(TrafficPathsDB.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS_SD);
        onCreate(database);
    }
}

