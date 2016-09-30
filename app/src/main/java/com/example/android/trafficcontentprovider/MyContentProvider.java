package com.example.android.trafficcontentprovider;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import com.example.android.database.LoggingDB;
import com.example.android.database.NotificationsDSDB;
import com.example.android.database.NotificationsSDDB;
import com.example.android.database.TrafficDatabaseHelper;
import com.example.android.database.TrafficPathsDB;
import com.example.android.database.TrafficPathsDB;

import java.util.Arrays;
import java.util.HashSet;

/**
 * Created by niarora on 11/3/2015.
 */
public class MyContentProvider extends ContentProvider {

    private TrafficDatabaseHelper databaseHelper;

    //codes for URI Matcher
    private static final int TRAFFICS = 10;
    private static final int TRAFFIC_ID = 20;
    private static final int NOTSD = 30;
    private static final int NOTSD_ID = 40;
    private static final int NOTDS = 50;
    private static final int NOTDS_ID = 60;
    private static final int LOG = 70;
    private static final int LOG_ID = 80;

    private static final String AUTHORITY = "com.example.android.trafficcontentprovider";
    private static final String BASE_PATH = "trafficpaths";
    private static final String BASE_PATH_NOTSD = "notificationsSD";
    private static final String BASE_PATH_NOTDS = "notificationsDS";
    private static final String BASE_PATH_LOGGING = "logging";

    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH);
    public static final Uri CONTENT_URI_NOTSD = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_NOTSD);
    public static final Uri CONTENT_URI_NOTDS = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_NOTDS);
    public static final Uri CONTENT_URI_LOGS = Uri.parse("content://" + AUTHORITY
            + "/" + BASE_PATH_LOGGING);

    public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/traffics";
    public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/traffic";

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, BASE_PATH, TRAFFICS);
        sURIMatcher.addURI(AUTHORITY, BASE_PATH + "/#", TRAFFIC_ID);
        sURIMatcher.addURI(AUTHORITY,BASE_PATH_NOTSD, NOTSD);
        sURIMatcher.addURI(AUTHORITY,BASE_PATH_NOTSD + "/#", NOTSD_ID);
        sURIMatcher.addURI(AUTHORITY,BASE_PATH_NOTDS, NOTDS);
        sURIMatcher.addURI(AUTHORITY,BASE_PATH_NOTDS + "/#", NOTDS_ID);
        sURIMatcher.addURI(AUTHORITY,BASE_PATH_LOGGING, LOG);
        sURIMatcher.addURI(AUTHORITY,BASE_PATH_LOGGING + "/#", LOG_ID);
    }

    @Override
    public boolean onCreate() {
        databaseHelper = new TrafficDatabaseHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        int uriType = sURIMatcher.match(uri);

        switch(uriType){
            case TRAFFICS:
                checkColumns(projection, TrafficPathsDB.TABLE_TRAFFICPATHS);
                queryBuilder.setTables(TrafficPathsDB.TABLE_TRAFFICPATHS);
                break;
            case TRAFFIC_ID:
                checkColumns(projection, TrafficPathsDB.TABLE_TRAFFICPATHS);
                queryBuilder.setTables(TrafficPathsDB.TABLE_TRAFFICPATHS);
                queryBuilder.appendWhere(TrafficPathsDB.COLUMN_ID + " =" + uri.getLastPathSegment());
                break;
            case NOTSD:
                checkColumns(projection, NotificationsSDDB.TABLE_NOTIFICATIONS_SD);
                queryBuilder.setTables(NotificationsSDDB.TABLE_NOTIFICATIONS_SD);
                break;
            case NOTSD_ID: //probably not needed for insert
                checkColumns(projection, NotificationsSDDB.TABLE_NOTIFICATIONS_SD);
                queryBuilder.setTables(NotificationsSDDB.TABLE_NOTIFICATIONS_SD);
                queryBuilder.appendWhere(NotificationsSDDB.COLUMN_ID + " =" + uri.getLastPathSegment());
                break;
            case NOTDS:
                checkColumns(projection, NotificationsDSDB.TABLE_NOTIFICATIONS_DS);
                queryBuilder.setTables(NotificationsDSDB.TABLE_NOTIFICATIONS_DS);
                break;
            case NOTDS_ID: //probably not needed for insert
                checkColumns(projection, NotificationsDSDB.TABLE_NOTIFICATIONS_DS);
                queryBuilder.setTables(NotificationsDSDB.TABLE_NOTIFICATIONS_DS);
                queryBuilder.appendWhere(NotificationsDSDB.COLUMN_ID + " =" + uri.getLastPathSegment());
                break;
            case LOG:
                //checkColumns(projection, LoggingDB.TABLE_LOGGING);
                queryBuilder.setTables(LoggingDB.TABLE_LOGGING);
                break;
            case LOG_ID: //probably not needed for insert
                checkColumns(projection, LoggingDB.TABLE_LOGGING);
                queryBuilder.setTables(LoggingDB.TABLE_LOGGING);
                queryBuilder.appendWhere(NotificationsDSDB.COLUMN_ID + " =" + uri.getLastPathSegment());
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        Cursor cursor = queryBuilder.query(db,projection,selection,selectionArgs,null, null, sortOrder);
        // make sure that potential listeners are getting notified
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = databaseHelper.getWritableDatabase();
        long id = 0;
        switch (uriType) {
            case TRAFFICS:
                id = sqlDB.insert(TrafficPathsDB.TABLE_TRAFFICPATHS, null, values);
                break;
            case NOTSD:
                id = sqlDB.insert(NotificationsSDDB.TABLE_NOTIFICATIONS_SD, null, values);
                break;
            case NOTDS:
                id = sqlDB.insert(NotificationsDSDB.TABLE_NOTIFICATIONS_DS, null, values);
                break;
            case LOG:
                id = sqlDB.insert(LoggingDB.TABLE_LOGGING, null, values);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return Uri.parse(BASE_PATH + "/" + id);
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int rowsDeleted = 0;
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = databaseHelper.getWritableDatabase();
        String id = null;
        switch (uriType){
            case TRAFFICS://delete complete table
                rowsDeleted = sqlDB.delete(TrafficPathsDB.TABLE_TRAFFICPATHS, selection,
                                            selectionArgs);
                break;
            case TRAFFIC_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = sqlDB.delete(TrafficPathsDB.TABLE_TRAFFICPATHS,
                                            TrafficPathsDB.COLUMN_ID + "=" + id, selectionArgs);
                break;
            case NOTSD:
                rowsDeleted = sqlDB.delete(NotificationsSDDB.TABLE_NOTIFICATIONS_SD, selection, selectionArgs);
                break;
            case NOTSD_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = sqlDB.delete(NotificationsSDDB.TABLE_NOTIFICATIONS_SD,
                                    NotificationsSDDB.COLUMN_ID + "=" + id, selectionArgs);
                break;
            case NOTDS:
                rowsDeleted = sqlDB.delete(NotificationsDSDB.TABLE_NOTIFICATIONS_DS, selection, selectionArgs);
                break;
            case NOTDS_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = sqlDB.delete(NotificationsDSDB.TABLE_NOTIFICATIONS_DS,
                        NotificationsDSDB.COLUMN_ID + "=" + id, selectionArgs);
                break;
            case LOG:
                rowsDeleted = sqlDB.delete(LoggingDB.TABLE_LOGGING, selection, selectionArgs);
                break;
            case LOG_ID:
                id = uri.getLastPathSegment();
                rowsDeleted = sqlDB.delete(LoggingDB.TABLE_LOGGING,
                        NotificationsDSDB.COLUMN_ID + "=" + id, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsDeleted;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        int rowsUpdated = 0;
        int uriType = sURIMatcher.match(uri);
        SQLiteDatabase sqlDB = databaseHelper.getWritableDatabase();
        String id = null;
        switch(uriType){
            case TRAFFIC_ID:
                id = uri.getLastPathSegment();
                rowsUpdated = sqlDB.update(TrafficPathsDB.TABLE_TRAFFICPATHS,
                                            values,
                                            TrafficPathsDB.COLUMN_ID + "=" + id,
                                            null);
                break;
            case NOTSD_ID:
                id = uri.getLastPathSegment();
                rowsUpdated = sqlDB.update(NotificationsSDDB.TABLE_NOTIFICATIONS_SD,
                        values,
                        NotificationsSDDB.COLUMN_ID + "=" + id,
                        null);
                break;
            case NOTDS_ID:
                id = uri.getLastPathSegment();
                rowsUpdated = sqlDB.update(NotificationsDSDB.TABLE_NOTIFICATIONS_DS,
                        values,
                        NotificationsDSDB.COLUMN_ID + "=" + id,
                        null);
                break;
            case LOG:
                rowsUpdated = sqlDB.update(LoggingDB.TABLE_LOGGING,
                        values,
                        selection,
                        null);
                break;
            case LOG_ID:
                id = uri.getLastPathSegment();
                rowsUpdated = sqlDB.update(LoggingDB.TABLE_LOGGING,
                        values,
                        LoggingDB.COLUMN_ID + "=" + id,
                        null);
                break;
        }
        getContext().getContentResolver().notifyChange(uri, null);
        return rowsUpdated;
    }

    private void checkColumns(String[] projection, String table) {
        HashSet<String> availableColumns;
        if(table.compareTo(TrafficPathsDB.TABLE_TRAFFICPATHS) == 0) {
            String[] available = {TrafficPathsDB.SOURCE_ADDRESS,
                    TrafficPathsDB.DEST_ADDRESS,
                    TrafficPathsDB.SOURCE_LATLNG,
                    TrafficPathsDB.DEST_LATLNG,
                    TrafficPathsDB.NOTIFICATIONS_ENABLED,
                    TrafficPathsDB.LOGGING_ENABLED,
                    TrafficPathsDB.COLUMN_ID};
            availableColumns = new HashSet<>(Arrays.asList(available));
        }
        else if(table.compareTo(NotificationsSDDB.TABLE_NOTIFICATIONS_SD) == 0){

            String[] available = {NotificationsSDDB.MONDAY_SD,
                    NotificationsSDDB.TUESDAY_SD,
                    NotificationsSDDB.WEDNESDAY_SD,
                    NotificationsSDDB.THURSDAY_SD,
                    NotificationsSDDB.FRIDAY_SD,
                    NotificationsSDDB.SATURDAY_SD,
                    NotificationsSDDB.SUNDAY_SD,
                    NotificationsSDDB.COLUMN_ID};
            availableColumns = new HashSet<>(Arrays.asList(available));
        }
        else if(table.compareTo(NotificationsDSDB.TABLE_NOTIFICATIONS_DS) == 0){

            String[] available = {NotificationsDSDB.MONDAY_DS,
                    NotificationsDSDB.TUESDAY_DS,
                    NotificationsDSDB.WEDNESDAY_DS,
                    NotificationsDSDB.THURSDAY_DS,
                    NotificationsDSDB.FRIDAY_DS,
                    NotificationsDSDB.SATURDAY_DS,
                    NotificationsDSDB.SUNDAY_DS,
                    NotificationsDSDB.COLUMN_ID};
            availableColumns = new HashSet<>(Arrays.asList(available));
        }
        else if(table.compareTo(LoggingDB.TABLE_LOGGING) == 0){

            String[] available = {LoggingDB.COLUMN_ID,
                    LoggingDB.DIRECTION,
                    LoggingDB.DAY,
                    LoggingDB.DATE,
                    LoggingDB.DURATION,
                    LoggingDB.TIME};
            availableColumns = new HashSet<>(Arrays.asList(available));
        }
        else {
            throw new IllegalArgumentException("Unknown columns in projection");
        }
        if (projection != null) {
            HashSet<String> requestedColumns = new HashSet<String>(Arrays.asList(projection));
            // check if all columns which are requested are available
            if (!availableColumns.containsAll(requestedColumns)) {
                throw new IllegalArgumentException("Unknown columns in projection");
            }
        }
    }
}
