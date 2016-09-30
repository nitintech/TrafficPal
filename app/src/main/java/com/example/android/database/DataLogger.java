package com.example.android.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import com.example.android.trafficcontentprovider.MyContentProvider;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by niarora on 7/26/2016.
 */
public class DataLogger {
    private static DataLogger m_Logger;
    private static final String TAG = "DataLogger";
    private static final long WEEK_SECONDS = 7*24*60*60;

    public static DataLogger getLogger() {
        if (m_Logger == null)
            m_Logger = new DataLogger();
        return m_Logger;
    }

    public int getLogCount(Context context, int col_id) {
        String[] projection = {"count(" + LoggingDB.DATE + ")"};
        int rowCount = 0;
        String whereClause = LoggingDB.COLUMN_ID + " = " + col_id;
        Cursor cursor = context.getContentResolver().query(MyContentProvider.CONTENT_URI_LOGS,
                projection,
                whereClause,
                null,
                null);
        cursor.moveToFirst();
        if(cursor.getCount() != 0) {
            rowCount = cursor.getInt(0);
        }
        return rowCount;
    }

    public synchronized boolean addData(int duration, int time, int col_id, int direction, Context context) {
        //current date
        Calendar presentCal = Calendar.getInstance();
        int dayOfWeek = presentCal.get(Calendar.DAY_OF_WEEK); //

        long julianTime = presentCal.getTimeInMillis()/1000;
        //SimpleDateFormat formatter=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //String strDate = formatter.format(presentCal.getTime());
        ContentValues values = new ContentValues();
        values.put(LoggingDB.COLUMN_ID, col_id);
        values.put(LoggingDB.DATE, julianTime);
        values.put(LoggingDB.TIME, time);
        values.put(LoggingDB.DIRECTION, direction);
        values.put(LoggingDB.DAY, dayOfWeek);
        values.put(LoggingDB.DURATION, duration);

        context.getContentResolver().insert(MyContentProvider.CONTENT_URI_LOGS, values);
        Log.d(TAG, "Added duration:" + duration + " for time:" + time + " for col_id:" + col_id);
        if (checkWeekData(col_id, context)) {
            //stop the logging here
            return false;
        }
        return true;
    }

    public void deleteData(int col_id, Context context) {
        Uri uri = Uri.parse(MyContentProvider.CONTENT_URI_LOGS + "/" + col_id);
        context.getContentResolver().delete(uri, null, null);
    }

    public Map<Integer, Integer> selectDataForDate(int col_id, int direction, Context context, Calendar calendar) {
        Map<Integer, Integer> map = new HashMap<>();
        //select time,duration from table where col_id, direction, 
        String[] projection = {LoggingDB.COLUMN_ID,
                LoggingDB.DIRECTION,
                LoggingDB.DAY,
                LoggingDB.DATE,
                LoggingDB.DURATION,
                LoggingDB.TIME};

        String whereClause = LoggingDB.COLUMN_ID + " = " + col_id + " and "
                + LoggingDB.DIRECTION + " = " + direction + " and " +
                " strftime('%s', strftime('%Y-%m-%d'," + LoggingDB.DATE + ", 'unixepoch')) = " + calendar.getTimeInMillis()/1000;

        Cursor cursor = context.getContentResolver().query(MyContentProvider.CONTENT_URI_LOGS,
                                            projection,
                                            whereClause,
                                            null,
                                            null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            if (!cursor.isNull(cursor.getColumnIndex(LoggingDB.COLUMN_ID))) {
                int time = cursor.getInt(cursor.getColumnIndex(LoggingDB.TIME));
                int duration = cursor.getInt(cursor.getColumnIndex(LoggingDB.DURATION));
                map.put(time, duration);
            }
            cursor.moveToNext();
        }
        cursor.close();
        return map;
    }
    public Map<Integer, Integer> selectDataForDay(int col_id, int direction, Context context, int day) {
        Map<Integer, Integer> map = new HashMap<>();
        //select time,duration from table where col_id, direction,
        String[] projection = {LoggingDB.COLUMN_ID,
                LoggingDB.DIRECTION,
                LoggingDB.DAY,
                LoggingDB.DATE,
                LoggingDB.DURATION,
                LoggingDB.TIME};

        String whereClause = LoggingDB.COLUMN_ID + " = " + col_id + " and " +
                LoggingDB.DIRECTION + " = " + direction + " and " +
                LoggingDB.DAY + " = " + day;

        Cursor cursor = context.getContentResolver().query(MyContentProvider.CONTENT_URI_LOGS,
                projection,
                whereClause,
                null,
                null);

        cursor.moveToFirst();
        int counter = 0;
        while (!cursor.isAfterLast()) {
            if (!cursor.isNull(cursor.getColumnIndex(LoggingDB.COLUMN_ID))) {

                int time = cursor.getInt(cursor.getColumnIndex(LoggingDB.TIME));
                int duration = cursor.getInt(cursor.getColumnIndex(LoggingDB.DURATION));

                if (map.containsKey(time)) {
                    map.put(time, ((map.get(time) * counter) + duration)/(counter+1));
                }
                else {
                    map.put(time, duration);
                }
                counter++;
            }
            cursor.moveToNext();
        }
        cursor.close();
        return map;
    }

    //returns True if done with a week's data
    private boolean checkWeekData(int col_id, Context context) {
        String[] projection = {"min(" + LoggingDB.DATE + ")"};
        String whereClause = LoggingDB.COLUMN_ID + " = " + col_id;
        Cursor cursor = context.getContentResolver().query(MyContentProvider.CONTENT_URI_LOGS,
                projection,
                whereClause,
                null,
                null);
        cursor.moveToFirst();
        if(cursor.getCount() == 0) return false;

        long firstTime = cursor.getInt(0);

        Calendar calendar = Calendar.getInstance();
        long curTime = calendar.getTimeInMillis()/1000;
        Log.d(TAG, "first time = " + firstTime + " cur time = " + curTime);
        cursor.close();
        if (curTime-firstTime > WEEK_SECONDS) return true;
        else return false;
    }

}
