package com.example.android.trafficpal;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


import com.example.android.database.DataLogger;
import com.example.android.database.NotificationsDSDB;
import com.example.android.database.NotificationsSDDB;
import com.example.android.database.TrafficPathsDB;
import com.example.android.notifications.AlarmScheduler;
import com.example.android.trafficcontentprovider.MyContentProvider;
import com.google.android.gms.maps.model.LatLng;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.Key;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>{

    private TextView mTextView_norts;
    private int mRouteCount = 0;
    private FloatingActionButton mAddRouteBtn;
    private Context mContext;
    private static final int REQUEST_ADD_ROUTE = 10;
    private static final int REQUEST_EDIT_ROUTE = 20;
    private static final int REQUEST_ROUTE_SETTINGS = 50;
    private static final String TAG = "MainActivity";
    private ListView listView_routes;
    private List<TrafficPaths> trafficList = new ArrayList<>();
    private List<NotificationTimings> list_timingsSD = new ArrayList<>();
    private List<NotificationTimings> list_timingsDS = new ArrayList<>();
    RouteAdapter mAdapter;
    private Toolbar mToolbar;
    private static final char TABLE_PATHS = 1<<0;
    private static final char TABLE_SDNOTS = 1<<1;
    private static final char TABLE_DSNOTS = 1<<2;
    private static final char ALL_DATA_LOADED = TABLE_PATHS|TABLE_SDNOTS|TABLE_DSNOTS;
    private static char mDataLoaded = 0;
    private boolean mShowActions = false;
    private int mSelectedIndex = -1;
    private boolean avoidHighways, avoidTolls, avoidFerries;
    private static final boolean DBG = false;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        CursorLoader cursorLoader = null;
        if(id == 0) {
            String[] projection = {TrafficPathsDB.COLUMN_ID,
                    TrafficPathsDB.SOURCE_ADDRESS,
                    TrafficPathsDB.DEST_ADDRESS,
                    TrafficPathsDB.SOURCE_LATLNG,
                    TrafficPathsDB.DEST_LATLNG,
                    TrafficPathsDB.LOGGING_ENABLED,
                    TrafficPathsDB.NOTIFICATIONS_ENABLED
            };
            cursorLoader = new CursorLoader(this, MyContentProvider.CONTENT_URI, projection, null, null, null);
        }
        else if(id == 1){
            String[] projection = {NotificationsSDDB.COLUMN_ID,
                    NotificationsSDDB.MONDAY_SD,
                    NotificationsSDDB.TUESDAY_SD,
                    NotificationsSDDB.WEDNESDAY_SD,
                    NotificationsSDDB.THURSDAY_SD,
                    NotificationsSDDB.FRIDAY_SD,
                    NotificationsSDDB.SATURDAY_SD,
                    NotificationsSDDB.SUNDAY_SD
            };
            cursorLoader = new CursorLoader(this, MyContentProvider.CONTENT_URI_NOTSD, projection, null, null, null);
        }
        else if(id == 2){
            String[] projection = {NotificationsDSDB.COLUMN_ID,
                    NotificationsDSDB.MONDAY_DS,
                    NotificationsDSDB.TUESDAY_DS,
                    NotificationsDSDB.WEDNESDAY_DS,
                    NotificationsDSDB.THURSDAY_DS,
                    NotificationsDSDB.FRIDAY_DS,
                    NotificationsDSDB.SATURDAY_DS,
                    NotificationsDSDB.SUNDAY_DS
            };
            cursorLoader = new CursorLoader(this, MyContentProvider.CONTENT_URI_NOTDS, projection, null, null, null);
        }
        return cursorLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(DBG) Log.d(TAG, "received back records from DB: size" + data.getCount() + "id=" + loader.getId());
        if(data == null || data.getCount() == 0)
            return;
        if(loader.getId() == 0) {
            trafficList.clear();

            data.moveToFirst();
            while (!data.isAfterLast()) {
                //process the result and store to the list
                String srcAddr = data.getString(data.getColumnIndex(TrafficPathsDB.SOURCE_ADDRESS));
                String destAddr = data.getString(data.getColumnIndex(TrafficPathsDB.DEST_ADDRESS));
                String srcLatlng = data.getString(data.getColumnIndex(TrafficPathsDB.SOURCE_LATLNG));
                String destLatlng = data.getString(data.getColumnIndex(TrafficPathsDB.DEST_LATLNG));
                boolean nots = data.getInt(data.getColumnIndex(TrafficPathsDB.NOTIFICATIONS_ENABLED)) == 1;
                boolean logging = data.getInt(data.getColumnIndex(TrafficPathsDB.LOGGING_ENABLED)) == 1;
                TrafficPaths tp = new TrafficPaths(srcAddr,
                        destAddr,
                        logging,
                        nots,
                        null,
                        null,
                        new LatLng(Double.parseDouble(srcLatlng.split(",")[0]), Double.parseDouble(srcLatlng.split(",")[1])),
                        new LatLng(Double.parseDouble(destLatlng.split(",")[0]), Double.parseDouble(destLatlng.split(",")[1])));
                int id = data.getInt(data.getColumnIndex(TrafficPathsDB.COLUMN_ID));
                tp.id = id;
                trafficList.add(tp);
                data.moveToNext();
            }
            //do it from UI thread??
            mAdapter.notifyDataSetChanged();
            setInterface();
            mDataLoaded |= TABLE_PATHS;
        }
        else if(loader.getId() == 1){
            //not SD load here
            data.moveToFirst();
            Iterator<TrafficPaths> iterator = trafficList.iterator();
            list_timingsSD.clear();

            while(!data.isAfterLast()){
                NotificationTimings nt = new NotificationTimings(NotificationTimings.DIRECTION_SD);
                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.MONDAY_SD))){
                    nt.setMillis(NotificationTimings.MONDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.MONDAY_SD)));
                    nt.setEnabled(NotificationTimings.MONDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.TUESDAY_SD))){
                    nt.setMillis(NotificationTimings.TUESDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.TUESDAY_SD)));
                    nt.setEnabled(NotificationTimings.TUESDAY, true);
                }

                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.WEDNESDAY_SD))){
                    nt.setMillis(NotificationTimings.WEDNESDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.WEDNESDAY_SD)));
                    nt.setEnabled(NotificationTimings.WEDNESDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.THURSDAY_SD))){
                    nt.setMillis(NotificationTimings.THURSDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.THURSDAY_SD)));
                    nt.setEnabled(NotificationTimings.THURSDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.FRIDAY_SD))){
                    nt.setMillis(NotificationTimings.FRIDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.FRIDAY_SD)));
                    nt.setEnabled(NotificationTimings.FRIDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.SATURDAY_SD))){
                    nt.setMillis(NotificationTimings.SATURDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.SATURDAY_SD)));
                    nt.setEnabled(NotificationTimings.SATURDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsSDDB.SUNDAY_SD))){
                    nt.setMillis(NotificationTimings.SUNDAY, data.getLong(data.getColumnIndex(NotificationsSDDB.SUNDAY_SD)));
                    nt.setEnabled(NotificationTimings.SUNDAY, true);
                }
                list_timingsSD.add(nt);
                /*
                if(iterator.hasNext())
                    iterator.next().mNotificationSD = nt;*/
                data.moveToNext();

            }
            mDataLoaded |= TABLE_SDNOTS;
        }

        else if(loader.getId() == 2){
            //not SD load here
            data.moveToFirst();
            Iterator<TrafficPaths> iterator = trafficList.iterator();
            list_timingsDS.clear();

            while(!data.isAfterLast()){
                NotificationTimings nt = new NotificationTimings(NotificationTimings.DIRECTION_DS);
                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.MONDAY_DS))){
                    nt.setMillis(NotificationTimings.MONDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.MONDAY_DS)));
                    nt.setEnabled(NotificationTimings.MONDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.TUESDAY_DS))){
                    nt.setMillis(NotificationTimings.TUESDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.TUESDAY_DS)));
                    nt.setEnabled(NotificationTimings.TUESDAY, true);
                }

                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.WEDNESDAY_DS))){
                    nt.setMillis(NotificationTimings.WEDNESDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.WEDNESDAY_DS)));
                    nt.setEnabled(NotificationTimings.WEDNESDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.THURSDAY_DS))){
                    nt.setMillis(NotificationTimings.THURSDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.THURSDAY_DS)));
                    nt.setEnabled(NotificationTimings.THURSDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.FRIDAY_DS))){
                    nt.setMillis(NotificationTimings.FRIDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.FRIDAY_DS)));
                    nt.setEnabled(NotificationTimings.FRIDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.SATURDAY_DS))){
                    nt.setMillis(NotificationTimings.SATURDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.SATURDAY_DS)));
                    nt.setEnabled(NotificationTimings.SATURDAY, true);
                }
                if(!data.isNull(data.getColumnIndex(NotificationsDSDB.SUNDAY_DS))){
                    nt.setMillis(NotificationTimings.SUNDAY, data.getLong(data.getColumnIndex(NotificationsDSDB.SUNDAY_DS)));
                    nt.setEnabled(NotificationTimings.SUNDAY, true);
                }
                list_timingsDS.add(nt);
                /*
                if(iterator.hasNext())
                    iterator.next().mNotificationDS = nt;*/
                data.moveToNext();

            }
            mDataLoaded |= TABLE_DSNOTS;
        }
        //if all data is loaded associate the notification timings and the routes
        if(mDataLoaded == ALL_DATA_LOADED) {
            Iterator<TrafficPaths> iteratorPaths = trafficList.iterator();
            Iterator<NotificationTimings> iteratorSD = list_timingsSD.iterator();
            Iterator<NotificationTimings> iteratorDS = list_timingsDS.iterator();
            while (iteratorPaths.hasNext()) {
                TrafficPaths tp = iteratorPaths.next();
                if (iteratorSD.hasNext()) {
                    tp.mNotificationSD = iteratorSD.next();
                }
                if (iteratorDS.hasNext())
                    tp.mNotificationDS = iteratorDS.next();
            }
            mDataLoaded = 0;
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    //class for saving all data related to saved form
    private class TrafficPaths{
        NotificationTimings mNotificationSD;
        NotificationTimings mNotificationDS;
        String sourceAddress;
        String destinationAddress;
        boolean enableAlerts;
        boolean enableLogging;
        LatLng latlngSource;
        LatLng latLngDest;
        int id;

        //constructor
        TrafficPaths(String src, String dest, boolean logging, boolean notify,
                     NotificationTimings mNotnewSD, NotificationTimings mNotnewDS,
                     LatLng srcLL, LatLng destLL){
            mNotificationSD = mNotnewSD;
            mNotificationDS = mNotnewDS;
            enableLogging = logging;
            enableAlerts = notify;
            sourceAddress = src;
            destinationAddress = dest;
            latlngSource = srcLL;
            latLngDest = destLL;
        }
    }

    private int addNewPathToDB(TrafficPaths tp){
        int status = 0;
        ContentValues values = new ContentValues();
        values.put(TrafficPathsDB.SOURCE_ADDRESS, tp.sourceAddress);
        values.put(TrafficPathsDB.DEST_ADDRESS, tp.destinationAddress);
        values.put(TrafficPathsDB.SOURCE_LATLNG, new StringBuilder().append(tp.latlngSource.latitude).append(",").append(tp.latlngSource.longitude).toString());
        values.put(TrafficPathsDB.DEST_LATLNG, new StringBuilder().append(tp.latLngDest.latitude).append(",").append(tp.latLngDest.longitude).toString());
        values.put(TrafficPathsDB.LOGGING_ENABLED, tp.enableLogging == true ? 1 : 0);
        values.put(TrafficPathsDB.NOTIFICATIONS_ENABLED, tp.enableAlerts == true ? 1 : 0);
        Uri returnVal = getContentResolver().insert(MyContentProvider.CONTENT_URI, values);
        try{
            status = Integer.parseInt(returnVal.getLastPathSegment());
        }
        catch(NumberFormatException e){
            Log.e(TAG, "Could not add to entry to db" + e.toString());
        }
        if(DBG) Log.d(TAG, "inserted at index:" + returnVal.getLastPathSegment());
        tp.id = status;

        //insert the notifications SD row now
        ContentValues valuesNotSD = new ContentValues();
        valuesNotSD.put(NotificationsSDDB.COLUMN_ID, status);
        if(tp.mNotificationSD.isEnabled(NotificationTimings.MONDAY)){
            valuesNotSD.put(NotificationsSDDB.MONDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.MONDAY));
        }
        if(tp.mNotificationSD.isEnabled(NotificationTimings.TUESDAY)){
            valuesNotSD.put(NotificationsSDDB.TUESDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.TUESDAY));
        }
        if(tp.mNotificationSD.isEnabled(NotificationTimings.WEDNESDAY)){
            valuesNotSD.put(NotificationsSDDB.WEDNESDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.WEDNESDAY));
        }
        if(tp.mNotificationSD.isEnabled(NotificationTimings.THURSDAY)){
            valuesNotSD.put(NotificationsSDDB.THURSDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.THURSDAY));
        }
        if(tp.mNotificationSD.isEnabled(NotificationTimings.FRIDAY)){
            valuesNotSD.put(NotificationsSDDB.FRIDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.FRIDAY));
        }
        if(tp.mNotificationSD.isEnabled(NotificationTimings.SATURDAY)){
            valuesNotSD.put(NotificationsSDDB.SATURDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.SATURDAY));
        }
        if(tp.mNotificationSD.isEnabled(NotificationTimings.SUNDAY)){
            valuesNotSD.put(NotificationsSDDB.SUNDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.SUNDAY));
        }
        getContentResolver().insert(MyContentProvider.CONTENT_URI_NOTSD, valuesNotSD);
        if(DBG) Log.d(TAG, "inserted notSD at:" + status);



        //insert the notifications DS row now
        ContentValues valuesNotDS = new ContentValues();
        valuesNotDS.put(NotificationsDSDB.COLUMN_ID, status);
        if(tp.mNotificationDS.isEnabled(NotificationTimings.MONDAY)){
            valuesNotDS.put(NotificationsDSDB.MONDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.MONDAY));
        }
        if(tp.mNotificationDS.isEnabled(NotificationTimings.TUESDAY)){
            valuesNotDS.put(NotificationsDSDB.TUESDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.TUESDAY));
        }
        if(tp.mNotificationDS.isEnabled(NotificationTimings.WEDNESDAY)){
            valuesNotDS.put(NotificationsDSDB.WEDNESDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.WEDNESDAY));
        }
        if(tp.mNotificationDS.isEnabled(NotificationTimings.THURSDAY)){
            valuesNotDS.put(NotificationsDSDB.THURSDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.THURSDAY));
        }
        if(tp.mNotificationDS.isEnabled(NotificationTimings.FRIDAY)){
            valuesNotDS.put(NotificationsDSDB.FRIDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.FRIDAY));
        }
        if(tp.mNotificationDS.isEnabled(NotificationTimings.SATURDAY)){
            valuesNotDS.put(NotificationsDSDB.SATURDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.SATURDAY));
        }
        if(tp.mNotificationDS.isEnabled(NotificationTimings.SUNDAY)){
            valuesNotDS.put(NotificationsDSDB.SUNDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.SUNDAY));
        }
        getContentResolver().insert(MyContentProvider.CONTENT_URI_NOTDS, valuesNotDS);
        if(DBG) Log.d(TAG, "inserted notDS at:" + status);
        return status;
    }

    private void deletePath(int index){
        //delete from traffic paths table first
        Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + index);
        int rowsDeleted;
        rowsDeleted = getContentResolver().delete(uri, null, null);
        if(DBG) Log.d(TAG, rowsDeleted + " rows deleted from traffic paths index " + index);

        //SD nots deletion
        uri = Uri.parse(MyContentProvider.CONTENT_URI_NOTSD + "/" + index);
        rowsDeleted = getContentResolver().delete(uri, null, null);
        if(DBG) Log.d(TAG, rowsDeleted + " rows deleted from NOT SD index " + index);
        //DS nots deletion
        uri = Uri.parse(MyContentProvider.CONTENT_URI_NOTDS + "/" + index);
        rowsDeleted = getContentResolver().delete(uri, null, null);
        if(DBG) Log.d(TAG, rowsDeleted + " rows deleted from NOT DS index " + index);

        DataLogger.getLogger().deleteData(index, this);
    }

    private void updatePath(int index){
        TrafficPaths tp = trafficList.get(index);
        ContentValues valuesPath = new ContentValues();
        valuesPath.put(TrafficPathsDB.LOGGING_ENABLED, tp.enableLogging);
        valuesPath.put(TrafficPathsDB.NOTIFICATIONS_ENABLED, tp.enableAlerts);

        Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + tp.id);
        int rowsUpdated;
        rowsUpdated = getContentResolver().update(uri, valuesPath, null, null);
        if(DBG) Log.d(TAG, rowsUpdated + " rows updated in trafficpaths index " + index);

        uri = Uri.parse(MyContentProvider.CONTENT_URI_NOTSD + "/" + tp.id);
        ContentValues valuesNotSD = new ContentValues();
        if(tp.mNotificationSD.isEnabled(NotificationTimings.MONDAY)){
            valuesNotSD.put(NotificationsSDDB.MONDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.MONDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.MONDAY_SD, (Long)null);
        }

        if(tp.mNotificationSD.isEnabled(NotificationTimings.TUESDAY)){
            valuesNotSD.put(NotificationsSDDB.TUESDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.TUESDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.TUESDAY_SD, (Long)null);
        }

        if(tp.mNotificationSD.isEnabled(NotificationTimings.WEDNESDAY)){
            valuesNotSD.put(NotificationsSDDB.WEDNESDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.WEDNESDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.WEDNESDAY_SD, (Long)null);
        }

        if(tp.mNotificationSD.isEnabled(NotificationTimings.THURSDAY)){
            valuesNotSD.put(NotificationsSDDB.THURSDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.THURSDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.THURSDAY_SD, (Long)null);
        }

        if(tp.mNotificationSD.isEnabled(NotificationTimings.FRIDAY)){
            valuesNotSD.put(NotificationsSDDB.FRIDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.FRIDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.FRIDAY_SD, (Long)null);
        }

        if(tp.mNotificationSD.isEnabled(NotificationTimings.SATURDAY)){
            valuesNotSD.put(NotificationsSDDB.SATURDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.SATURDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.SATURDAY_SD, (Long)null);
        }

        if(tp.mNotificationSD.isEnabled(NotificationTimings.SUNDAY)){
            valuesNotSD.put(NotificationsSDDB.SUNDAY_SD, tp.mNotificationSD.getMillis(NotificationTimings.SUNDAY));
        }
        else{
            valuesNotSD.put(NotificationsSDDB.SUNDAY_SD, (Long)null);
        }

        getContentResolver().update(uri, valuesNotSD, null, null);


        //insert the notifications DS row now
        uri = Uri.parse(MyContentProvider.CONTENT_URI_NOTDS + "/" + tp.id);
        ContentValues valuesNotDS = new ContentValues();
        if(tp.mNotificationDS.isEnabled(NotificationTimings.MONDAY)){
            valuesNotDS.put(NotificationsDSDB.MONDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.MONDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.MONDAY_DS, (Long) null);
        }

        if(tp.mNotificationDS.isEnabled(NotificationTimings.TUESDAY)){
            valuesNotDS.put(NotificationsDSDB.TUESDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.TUESDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.TUESDAY_DS, (Long) null);
        }

        if(tp.mNotificationDS.isEnabled(NotificationTimings.WEDNESDAY)){
            valuesNotDS.put(NotificationsDSDB.WEDNESDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.WEDNESDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.WEDNESDAY_DS, (Long) null);
        }

        if(tp.mNotificationDS.isEnabled(NotificationTimings.THURSDAY)){
            valuesNotDS.put(NotificationsDSDB.THURSDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.THURSDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.THURSDAY_DS, (Long) null);
        }

        if(tp.mNotificationDS.isEnabled(NotificationTimings.FRIDAY)){
            valuesNotDS.put(NotificationsDSDB.FRIDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.FRIDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.FRIDAY_DS, (Long) null);
        }

        if(tp.mNotificationDS.isEnabled(NotificationTimings.SATURDAY)){
            valuesNotDS.put(NotificationsDSDB.SATURDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.SATURDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.SATURDAY_DS, (Long) null);
        }

        if(tp.mNotificationDS.isEnabled(NotificationTimings.SUNDAY)){
            valuesNotDS.put(NotificationsDSDB.SUNDAY_DS, tp.mNotificationDS.getMillis(NotificationTimings.SUNDAY));
        }
        else{
            valuesNotDS.put(NotificationsDSDB.SUNDAY_DS, (Long) null);
        }

        getContentResolver().update(uri, valuesNotDS, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView_norts = (TextView) findViewById(R.id.textView_mainText);
        mAddRouteBtn = (FloatingActionButton) findViewById(R.id.button_add_route);
        listView_routes = (ListView) findViewById(R.id.listView_routes);

        /*if(getSupportActionBar() == null)
            Log.e(TAG, "Action bar is null");
        else
            GenericFunctions.setUpActionBarColor(this, getResources().getColor(R.color.color_blue));*/
        mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);
        setToolbarColor(R.color.color_pink);


        mAdapter = new RouteAdapter(getLayoutInflater());
        listView_routes.setAdapter(mAdapter);
        listView_routes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mShowActions) {
                    //listView_routes.getChildAt(mSelectedIndex).setBackground(new ColorDrawable(getResources().getColor(R.color.color_white)));
                    listView_routes.getChildAt(mSelectedIndex-listView_routes.getFirstVisiblePosition()).setSelected(false);
                }
                mShowActions = true;
                mSelectedIndex = position;
                invalidateOptionsMenu();
                //view.setBackground(new ColorDrawable(getResources().getColor(R.color.color_grey)));
                view.setSelected(true);
                return true;
            }
        });
        mContext = this;
        /*if(mTextView_norts != null && mRouteCount != 0){
            mTextView_norts.setVisibility(View.INVISIBLE);
        }*/
        mAddRouteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, AddRoute.class);
                startActivityForResult(intent, REQUEST_ADD_ROUTE);
            }
        });
        listView_routes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent x = new Intent(mContext, MapsActivity.class);
                TrafficPaths tp = trafficList.get(position);

                if(mShowActions){
                    mShowActions = false;

                    invalidateOptionsMenu();
                    //listView_routes.getChildAt(mSelectedIndex).setBackground(new ColorDrawable(getResources().getColor(R.color.color_white)));
                    listView_routes.getChildAt(mSelectedIndex-listView_routes.getFirstVisiblePosition()).setSelected(false);
                    mSelectedIndex = -1;
                }

                if(tp == null) {
                    Log.e(TAG, "Error getting item from list");
                    return;
                }
                x.putExtra("SOURCE", tp.sourceAddress);
                x.putExtra("DESTINATION", tp.destinationAddress);
                x.putExtra("LATITUDE_SOURCE", tp.latlngSource.latitude);
                x.putExtra("LATITUDE_DESTINATION", tp.latLngDest.latitude);
                x.putExtra("LONGITUDE_SOURCE", tp.latlngSource.longitude);
                x.putExtra("LONGITUDE_DESTINATION", tp.latLngDest.longitude);
                x.putExtra("AVOID_HIGHWAYS", avoidHighways);
                x.putExtra("AVOID_TOLLS", avoidTolls);
                x.putExtra("AVOID_FERRIES", avoidFerries);
                x.putExtra("COL_ID", tp.id);
                startActivity(x);
            }
        });

        //Initiate loader initialization to get the results from DB
        getLoaderManager().initLoader(0, null, this);
        //initiate loader initialization to get notifications also
        getLoaderManager().initLoader(1, null, this);
        //initiate loader initialization to get notifications also
        getLoaderManager().initLoader(2, null, this);

        //start the scheduler service
        Intent serviceIntent = new Intent(MainActivity.this, AlarmScheduler.class);
        serviceIntent.setFlags(0);
        startService(serviceIntent);

        //load the route settings or else set default one.
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(!sharedPreferences.contains("Highways")){
            avoidTolls = true;
            saveRouteSettings(false, true, false);
        }
        else{
            loadRouteSettings();
        }

    }

    private void sampleCode(){
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        final JsonFactory json = new AndroidJsonFactory();
        HttpRequestFactory factory = transport.createRequestFactory(new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest httpRequest) throws IOException {
                httpRequest.setParser(new JsonObjectParser(json));
            }
        });
        GenericUrl url = new GenericUrl("http://dev.virtualearth.net/REST/V1/Routes/Driving");
        //url.put("wp.0", "32.90, -117.20");
        url.put("wp.1","8840 Costa Verde Blvd, San Diego, CA 92122");
        url.put("wp.0", "9685 Scranton Rd, San Diego, CA 92121");
        url.put("avoid", "minimizeTolls");
        url.put("key", "AvobbgQXH--qR4QQw9dhr6jVY-J4ScO54-h2h_X3ZAV9AOd1ylJ9Fp06nk1icH8k");

        try {
            HttpRequest request = factory.buildGetRequest(url);
            HttpResponse response = request.execute();
            //String xx = response.parseAsString();
            //String[] xxx = response.parseAsString().split(":");
                    /*for(String y:xxx){
                        Log.d(TAG, y);
                    }*/
            //Log.d(TAG, "got the response:" + response.parseAsString());
            BingMaps bingMaps = response.parseAs(BingMaps.class);
            List<ResourcesSets> resourcesSet = bingMaps.resourceSets;
            List<ResourcesBing> resourcesBing =  resourcesSet.get(0).resources;
            ResourcesBing resultRoute = resourcesBing.get(0);
            if(DBG) Log.d(TAG, "congestion=" + resultRoute.congestion + " distance:"
                    + resultRoute.travelDist + " duration:" + resultRoute.duration + "traffic duration:" + resultRoute.durationTraffic );

        }
        catch (IOException e){
            Log.e(TAG, "Error creating http request");
        }
    }

    public static class BingMaps{
        @Key("resourceSets")
        public List<ResourcesSets> resourceSets;
    }

    public static class ResourcesSets{
        @Key("resources")
        public List<ResourcesBing> resources;
    }

    public static class ResourcesBing{
        @Key("trafficCongestion")
        public String congestion;
        @Key("travelDistance")
        public double travelDist;
        @Key("travelDuration")
        public double duration;
        @Key("travelDurationTraffic")
        public double durationTraffic;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_ADD_ROUTE){
            if(resultCode == RESULT_OK){
                Parcelable[] parcel= data.getParcelableArrayExtra("Notifications");
                NotificationTimings mNotnewSD = (NotificationTimings) parcel[0];
                NotificationTimings mNotnewDS = (NotificationTimings) parcel[1];
                if(mNotnewDS == null || mNotnewDS == null)
                    return;
                String src = data.getStringExtra("Source");
                String dest = data.getStringExtra("Destination");
                boolean notify = data.getBooleanExtra("Notify", false);
                boolean logging = data.getBooleanExtra("Logging", false);
                if(src == null || src.trim().compareTo("") == 0)
                    return;
                if(dest == null || dest.trim().compareTo("") == 0)
                    return;
                //get the Latitude and Longitude
                double srcLat = data.getDoubleExtra("srclat", -1);
                double srcLong = data.getDoubleExtra("srclong", -1);
                double destLat = data.getDoubleExtra("destlat", -1);
                double destLong = data.getDoubleExtra("destlong", -1);
                if(srcLat == -1 || srcLong == -1 || destLat == -1 || destLong == -1){
                    return;
                }


                TrafficPaths tp = new TrafficPaths(src, dest, logging, notify, mNotnewSD, mNotnewDS,
                        new LatLng(srcLat, srcLong), new LatLng(destLat, destLong));
                trafficList.add(tp);
                mAdapter.notifyDataSetChanged();
                setInterface();
                //also perform the SQL insertion here?
                addNewPathToDB(tp);

            }
        }
        else if(requestCode == REQUEST_EDIT_ROUTE && resultCode == RESULT_OK){
            //code to save the specific path. DB update
            if(data == null) return;
            int index = data.getIntExtra("Index", -1);
            if(index < 0) return;
            TrafficPaths tp = trafficList.get(index);
            Parcelable[] parcel= data.getParcelableArrayExtra("Notifications");
            NotificationTimings mNotnewSD = (NotificationTimings) parcel[0];
            NotificationTimings mNotnewDS = (NotificationTimings) parcel[1];
            if(mNotnewDS == null || mNotnewDS == null)
                return;

            boolean notify = data.getBooleanExtra("Notify", false);
            boolean logging = data.getBooleanExtra("Logging", false);

            if (tp.enableLogging == false && logging == true) {
                //delete all logged data from this path to collect new Data
                Log.d(TAG, "Deleting all logged data for col_id =" + tp.id);
                DataLogger.getLogger().deleteData(tp.id, this);
            }

            tp.enableAlerts = notify;
            tp.enableLogging = logging;
            tp.mNotificationSD = mNotnewSD;
            tp.mNotificationDS = mNotnewDS;
            mAdapter.notifyDataSetChanged();
            updatePath(index);
        }
        else if(requestCode == REQUEST_ROUTE_SETTINGS && resultCode == RESULT_OK){
            if(data == null) return;
            avoidHighways = data.getBooleanExtra("Highways", false);
            avoidTolls = data.getBooleanExtra("Tolls", false);
            avoidFerries = data.getBooleanExtra("Ferries", false);
            saveRouteSettings(avoidHighways, avoidTolls, avoidFerries);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        setInterface();
        super.onResume();
    }

    private void setInterface(){
        if(trafficList.size() >= 1){
            mTextView_norts.setVisibility(View.INVISIBLE);
            mTextView_norts.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 0
            ));
            listView_routes.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
            ));
        }
        else if(trafficList.size() == 0){
            //mTextView_norts.setVisibility(View.INVISIBLE);
            mTextView_norts.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1
            ));
            listView_routes.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 0
            ));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if(mShowActions) {
            menu.findItem(R.id.action_edit).setVisible(true);
            menu.findItem(R.id.action_delete).setVisible(true);

        }
        else{
            menu.findItem(R.id.action_edit).setVisible(false);
            menu.findItem(R.id.action_delete).setVisible(false);
            if(getActionBar() != null)
                setToolbarColor(R.color.color_pink);
        }

        return true;
    }

    private void setToolbarColor(int color){
        if(mToolbar!=null)
            mToolbar.setBackground(new ColorDrawable(getResources().getColor(color)));
    }

    @Override
    public void onBackPressed() {

        if(mShowActions){
            mShowActions = false;
            invalidateOptionsMenu();
            //listView_routes.getChildAt(mSelectedIndex).setBackground(new ColorDrawable(getResources().getColor(R.color.color_white)));
            listView_routes.getChildAt(mSelectedIndex -listView_routes.getFirstVisiblePosition()).setSelected(false);
            mSelectedIndex = -1;
        }
        else{
            super.onBackPressed();
        }
    }


    private void saveRouteSettings(boolean avoidHighways, boolean avoidTolls, boolean avoidFerries){
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("Highways", avoidHighways);
        editor.putBoolean("Tolls", avoidTolls);
        editor.putBoolean("Ferries", avoidFerries);

        editor.commit();
    }

    private int loadRouteSettings(){
        int retVal = 0;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//getPreferences(MODE_PRIVATE);
        avoidHighways = sharedPreferences.getBoolean("Highways", false);
        if(avoidHighways) retVal |= (1);

        avoidTolls = sharedPreferences.getBoolean("Tolls", false);
        if(avoidTolls) retVal |= 1<<1;

        avoidFerries = sharedPreferences.getBoolean("Ferries", false);
        if(avoidFerries) retVal |= 1<<2;

        return retVal;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            //display the activity to select the route option
            Intent intent = new Intent(MainActivity.this, RouteSettings.class);
            intent.putExtra("Highways", avoidHighways);
            intent.putExtra("Tolls", avoidTolls);
            intent.putExtra("Ferries", avoidFerries);
            startActivityForResult(intent, REQUEST_ROUTE_SETTINGS);
            return true;
        }
        else if (id == R.id.action_delete){
            if(mSelectedIndex != -1){
                //listView_routes.getChildAt(mSelectedIndex).setBackground(new ColorDrawable(getResources().getColor(R.color.color_white)));
                //listView_routes.getChildAt(mSelectedIndex).setSelected(false);
                TrafficPaths tp = trafficList.get(mSelectedIndex);
                int deleteIndex = tp.id;

                trafficList.remove(mSelectedIndex);
                mAdapter.notifyDataSetChanged();
                //setInterface();

                deletePath(deleteIndex);

                mSelectedIndex = -1;
                mShowActions = false;
                invalidateOptionsMenu();
                setInterface();
            }
        }
        else if(id == R.id.action_edit){

            TrafficPaths tp =trafficList.get(mSelectedIndex);
            Intent intent = new Intent(MainActivity.this, AddRoute.class);
            intent.putExtra("SourceAddress", tp.sourceAddress);
            intent.putExtra("DestAddress", tp.destinationAddress);
            intent.putExtra("IsNotify", tp.enableAlerts);
            intent.putExtra("isLogging", tp.enableLogging);
            intent.putExtra("SrcLatLng", tp.latlngSource);
            intent.putExtra("DestLatLng", tp.latLngDest);
            intent.putExtra("NotSD", tp.mNotificationSD);
            intent.putExtra("NotDS", tp.mNotificationDS);
            intent.putExtra("Index", mSelectedIndex);

            startActivityForResult(intent, REQUEST_EDIT_ROUTE);

            mSelectedIndex = -1;
            mShowActions = false;
            invalidateOptionsMenu();
        }

        return super.onOptionsItemSelected(item);
    }

    //class to manage the list view
    private class RouteAdapter extends BaseAdapter{

        private LayoutInflater mInflater;
        public RouteAdapter(LayoutInflater inflater){
            mInflater = inflater;
        }

        @Override
        public int getCount() {
            return trafficList.size();
        }

        @Override
        public Object getItem(int position) {
            return trafficList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewGroup vg;
            if(convertView != null){
                vg = (ViewGroup) convertView;
            }
            else{
                vg = (ViewGroup) mInflater.inflate(R.layout.routeslistview, null);
            }
            TextView txtSource = (TextView) vg.findViewById(R.id.textview_source);
            TextView txtDest = (TextView) vg.findViewById(R.id.textview_destination);
            //TextView txtProps = (TextView) vg.findViewById(R.id.textview_options);
            CheckBox box_nots = (CheckBox) vg.findViewById(R.id.box_listnots);
            CheckBox box_logs = (CheckBox) vg.findViewById(R.id.box_listlogs);
            //assuming we got the views
            TrafficPaths tp = trafficList.get(position);
            if(tp != null){
                String srcHtml = "<b>SOURCE: </b>"+ "<font color = \"blue\">" + tp.sourceAddress + "</font>";
                txtSource.setText(Html.fromHtml(srcHtml));

                String destHtml = "<b>DESTINATION: </b>"+ "<font color = \"blue\">" + tp.destinationAddress+ "</font>";
                txtDest.setText(Html.fromHtml(destHtml));

                box_nots.setChecked(tp.enableAlerts);
                box_logs.setChecked(tp.enableLogging);
            }
            return vg;
        }
    }

    //List click actions

}


