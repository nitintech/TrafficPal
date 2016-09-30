package com.example.android.database;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Created by niarora on 11/7/2015.
 */
public class NotificationsDSDB {

    public static final String TABLE_NOTIFICATIONS_DS = "Notifications_table_DS";
    public static final String COLUMN_ID = "_id";
    public static final String MONDAY_DS = "mondayds";
    public static final String TUESDAY_DS = "tuesdayds";
    public static final String WEDNESDAY_DS = "wednesdayds";
    public static final String THURSDAY_DS = "thursdayds";
    public static final String FRIDAY_DS = "fridayds";
    public static final String SATURDAY_DS = "saturdayds";
    public static final String SUNDAY_DS = "sundayds";


    // Database creation SQL statement
    private static final String DATABASE_CREATE = "create table "
            + TABLE_NOTIFICATIONS_DS
            + "("
            + COLUMN_ID + " integer not null, "        //0
            + MONDAY_DS + " integer, "                 //1
            + TUESDAY_DS + " integer ,"                //2
            + WEDNESDAY_DS + " integer, "              //3
            + THURSDAY_DS + " integer, "               //4
            + FRIDAY_DS + " integer, "                 //5
            + SATURDAY_DS + " integer, "               //6
            + SUNDAY_DS + " integer"                   //7
            + ");";

    public static void onCreate(SQLiteDatabase database){
        database.execSQL(DATABASE_CREATE);
    }

    public static void onUpgrade(SQLiteDatabase database, int oldVersion,
                                 int newVersion) {
        Log.w(TrafficPathsDB.class.getName(), "Upgrading database from version "
                + oldVersion + " to " + newVersion
                + ", which will destroy all old data");
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTIFICATIONS_DS);
        onCreate(database);
    }
}
