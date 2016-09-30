package com.example.android.notifications;

import android.app.AlarmManager;
import android.app.LoaderManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.android.database.DataLogger;
import com.example.android.database.LoggingDB;
import com.example.android.database.NotificationsDSDB;
import com.example.android.database.NotificationsSDDB;
import com.example.android.database.TrafficPathsDB;
import com.example.android.trafficcontentprovider.MyContentProvider;
import com.example.android.trafficpal.ChartViewer;
import com.example.android.trafficpal.MapsActivity;
import com.example.android.trafficpal.NotificationTimings;
import com.example.android.trafficpal.R;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmScheduler extends Service
{
    CursorLoader mLoaderSD;
    CursorLoader mLoaderDS;
    CursorLoader mLoaderPaths;
    private BroadcastReceiver mLoggingRcvr;
    private NotificationManager mNotManager;
    private List<PathInfo> mPathList = new ArrayList<>();
    private PendingIntent mIntentLogging;
    private static final String TAG = "AlarmScheduler";
    private static final String LOGGING_ACTION = "com.traffic.logging";
    private static final boolean DBG = false;
    private List<NotificationTimings> list_timingsSD = new ArrayList<>();
    private List<NotificationTimings> list_timingsDS = new ArrayList<>();
    public int[] dayMap = {0, 6, 0, 1, 2, 3, 4, 5};
    List<AlarmsDay> dailyAlarmsSD = new ArrayList<>();
    List<AlarmsDay> dailyAlarmsDS = new ArrayList<>();
    public static int LOGGING_INTERVAL = 10; //in minutes

    AlarmManager mAlarmManager;
    public static final int REQUEST_CODE_SD = 1;
    public static final int REQUEST_CODE_DS = 100;

    private static final int NOTIFICATION_ID_SD = 1;
    private static final int NOTIFICATION_ID_DS = 2;

    private static final int NOTIFICATION_ID_LOG = 3;

    public static int DIRECTION_SD = 0;
    public static int DIRECTION_DS = 1;
    private final IBinder mBinder = new MyBinder();
    public AlarmScheduler() {
    }

    class AlarmsDay{
        int id;
        Calendar calendar;
        PendingIntent pendingIntent;
    }

    private class PathInfo {
        boolean enableAlerts;
        boolean enableLogging;
        LatLng latlngSource;
        LatLng latLngDest;
        String src;
        String dest;
        int id;

        PathInfo(int col_id, LatLng srcLL, LatLng dstLL, boolean not, boolean log, String srcStr, String dstStr) {
            latlngSource = srcLL;
            latLngDest = dstLL;
            enableAlerts = not;
            enableLogging = log;
            id = col_id;
            src = srcStr;
            dest = dstStr;

        }
    }

    public class MyBinder extends Binder
    {
        AlarmScheduler getService(){
            return AlarmScheduler.this;
        }
    }

    public void prepareNotification(int id, int direction){
        if(DBG) Log.d(TAG, "prepareNotification:" + id + " direction:" + direction);
        boolean avoidHighways = false;
        boolean avoidTolls = false;
        //get the avoidances
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if(sharedPreferences.getBoolean("Highways", false)){
            avoidHighways = true;
        }
        if(sharedPreferences.getBoolean("Tolls", false)){
            avoidTolls = true;
        }
        if(DBG) Log.d(TAG, "firing notification, avoidhighways = " + avoidHighways + " and avoidTolls =" + avoidTolls);
        (new Thread(new NotificationCreater(id, direction, avoidHighways, avoidTolls,  this))).start();
    }

    private void fireAlarmsForLogging() {
        Calendar currCal = Calendar.getInstance();
        int currentHr = currCal.get(Calendar.HOUR_OF_DAY);
        int currentMin = currCal.get(Calendar.MINUTE);
        int slot = currentMin/LOGGING_INTERVAL;

        currentMin = (slot+1)*LOGGING_INTERVAL;
        if (currentMin >= 60){
            currentHr++;
            currentMin = 0;
        }

        Log.d(TAG, "The next logging alarm will be at hr:" + currentHr + " and min:" + currentMin);

        currCal.set(Calendar.HOUR_OF_DAY, currentHr);
        currCal.set(Calendar.MINUTE, currentMin);
        currCal.set(Calendar.SECOND, 0);
        currCal.set(Calendar.MILLISECOND, 0);

        //Start the repeating alarm
        Intent intent = new Intent();
        intent.setAction(LOGGING_ACTION);
        mIntentLogging = PendingIntent.getBroadcast(this, 0, intent,  PendingIntent.FLAG_UPDATE_CURRENT);
        //mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, currCal.getTimeInMillis(), LOGGING_INTERVAL * 60 * 1000, mIntentLogging);
        if (Build.VERSION.SDK_INT >= 23) {
            Log.d(TAG, "Setting allow while idle for sdk >= 23");
            mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, currCal.getTimeInMillis(), mIntentLogging);
        }
        else {
            mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, currCal.getTimeInMillis(), LOGGING_INTERVAL * 60 * 1000, mIntentLogging);
        }
        Log.d(TAG, "started logging alarm");
    }

    private void fireAlarms(int direction){
        List<AlarmsDay> alarmsFire = null;
        int requestCode = 0;
        if(direction == DIRECTION_SD) {
            alarmsFire = dailyAlarmsSD;
            requestCode = REQUEST_CODE_SD;
        }
        else{
            alarmsFire = dailyAlarmsDS;
            requestCode = REQUEST_CODE_DS;
        }
        for(AlarmsDay ad:alarmsFire){
            Intent intent = new Intent(AlarmScheduler.this, NotificationAlarmReceiver.class);
            intent.putExtra("ID", ad.id);
            intent.putExtra("DIRECTION", direction);
            //using separate codes for these pending intents to coexist
            PendingIntent pintent = PendingIntent.getBroadcast(getApplicationContext(), requestCode + ad.id, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            ad.pendingIntent = pintent;
            if(DBG) Log.d(TAG, "setting alarm for id:" + ad.id + " direction:" + direction + " hr:" + ad.calendar);
            mAlarmManager.setExact(AlarmManager.RTC_WAKEUP, ad.calendar.getTimeInMillis(), pintent);
        }
    }

    private void clearAllAlarms(int direction){
        List<AlarmsDay> alarmsFire = null;
        if(direction == DIRECTION_SD)
            alarmsFire = dailyAlarmsSD;
        else
            alarmsFire = dailyAlarmsDS;

        for(AlarmsDay ad:alarmsFire){
            if(ad.pendingIntent != null)
                mAlarmManager.cancel(ad.pendingIntent);
        }
        alarmsFire.clear();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
        //throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mAlarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        mNotManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        String[] projectionSD = {NotificationsSDDB.COLUMN_ID,
                NotificationsSDDB.MONDAY_SD,
                NotificationsSDDB.TUESDAY_SD,
                NotificationsSDDB.WEDNESDAY_SD,
                NotificationsSDDB.THURSDAY_SD,
                NotificationsSDDB.FRIDAY_SD,
                NotificationsSDDB.SATURDAY_SD,
                NotificationsSDDB.SUNDAY_SD
        };
        mLoaderSD = new CursorLoader(getApplicationContext(), MyContentProvider.CONTENT_URI_NOTSD, projectionSD, null, null, null);
        mLoaderSD.registerListener(0, new LoadSDNotificationListener());
        mLoaderSD.startLoading();

        String[] projectionDS = {NotificationsDSDB.COLUMN_ID,
                NotificationsDSDB.MONDAY_DS,
                NotificationsDSDB.TUESDAY_DS,
                NotificationsDSDB.WEDNESDAY_DS,
                NotificationsDSDB.THURSDAY_DS,
                NotificationsDSDB.FRIDAY_DS,
                NotificationsDSDB.SATURDAY_DS,
                NotificationsDSDB.SUNDAY_DS
        };
        mLoaderDS = new CursorLoader(getApplicationContext(), MyContentProvider.CONTENT_URI_NOTDS, projectionDS, null, null, null);
        mLoaderDS.registerListener(0, new LoadDSNotificationListener());
        mLoaderDS.startLoading();

        String[] projectTrafficPath = {
                TrafficPathsDB.COLUMN_ID,
                TrafficPathsDB.SOURCE_ADDRESS,
                TrafficPathsDB.DEST_ADDRESS,
                TrafficPathsDB.SOURCE_LATLNG,
                TrafficPathsDB.DEST_LATLNG,
                TrafficPathsDB.LOGGING_ENABLED,
                TrafficPathsDB.NOTIFICATIONS_ENABLED
        };

        mLoaderPaths = new CursorLoader(getApplicationContext(), MyContentProvider.CONTENT_URI, projectTrafficPath, null, null, null);
        mLoaderPaths.registerListener(0, new LoadPathsListner());
        mLoaderPaths.startLoading();

        //set a midnight repeating alarm here
        Intent intent = new Intent(this, MidnightAlarmReceiver.class);
        PendingIntent pint = PendingIntent.getBroadcast(this, 0, intent, 0);
        Calendar currentCal = Calendar.getInstance();

        currentCal.set(Calendar.HOUR_OF_DAY, 1); //1AM??
        currentCal.set(Calendar.MINUTE, 0);
        currentCal.set(Calendar.SECOND, 0);
        mAlarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, currentCal.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pint);
        if(DBG) Log.d(TAG, "midnight alarm set for 1am, calender:" + currentCal);

        mLoggingRcvr = new LoggingAlarmReceiver();
        getApplicationContext().registerReceiver(mLoggingRcvr, new IntentFilter(LOGGING_ACTION));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w(TAG, "onStartCommand");
        // this will be called every time we start the service (midnight everyday)
        //to get and set alarms of the day
        super.onStartCommand(intent, flags, startId);
        if(DBG) Log.d(TAG, "flags=" + flags);

        if(intent != null && intent.getFlags() == NotificationAlarmReceiver.FLAG_SETUP_NOTIFICATIONS){
            if(DBG) Log.d(TAG, "intent flags=" + intent.getFlags());
            int id = intent.getIntExtra("ID", -1);
            int direction = intent.getIntExtra("DIRECTION", -1);
            if(DBG) Log.d(TAG, "Prepate notification: " + id);
            if(id != -1 && direction != -1)
                prepareNotification(id, direction);
        }
        else if(intent != null && intent.getFlags() == MidnightAlarmReceiver.FLAG_MIDNIGHT_RELOAD){
            //reload the cursors
            if(mLoaderSD.isStarted()) {
                mLoaderSD.forceLoad();
                if(DBG) Log.d(TAG, "force reloaded SD nots alarms");
            }
            if(mLoaderDS.isStarted()) {
                mLoaderDS.forceLoad();
                if(DBG) Log.d(TAG, "force reloaded DS nots alarms");
            }
        }
        else if (intent != null && intent.getFlags() == LoggingAlarmReceiver.FLAG_START_LOGGING) {
            /*if (mLoggingRcvr != null) {
                getApplicationContext().unregisterReceiver(mLoggingRcvr);
            }*/

            //Start the repeating alarm


            //mAlarmManager.setRepeating(AlarmManager.RTC_WAKEUP, currCal.getTimeInMillis(), LOGGING_INTERVAL * 60 * 1000, mIntentLogging);
            if (Build.VERSION.SDK_INT >= 23) {
                Log.d(TAG, "Re-Setting allow while idle for sdk >= 23");
                Intent logIntent = new Intent();
                logIntent.setAction(LOGGING_ACTION);
                mIntentLogging = PendingIntent.getBroadcast(this, 0, logIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
                mAlarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,
                        Calendar.getInstance().getTimeInMillis() + LOGGING_INTERVAL*60*1000, mIntentLogging);
            }


            mLoggingRcvr = new LoggingAlarmReceiver();
            registerReceiver(mLoggingRcvr, new IntentFilter(LOGGING_ACTION));
            //get the avoidances
            boolean avoidHighways = false, avoidTolls = false;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            if(sharedPreferences.getBoolean("Highways", false)){
                avoidHighways = true;
            }
            if(sharedPreferences.getBoolean("Tolls", false)) {
                avoidTolls = true;
            }
            (new Thread(new DataBaseFeeder(getApplicationContext(), avoidHighways, avoidTolls))).start();
        }
        //here just clear alarms for the day and set the alarms for the day
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        if (mLoggingRcvr != null) {
            Log.d(TAG, "Unregister Receiver");
            getApplicationContext().unregisterReceiver(mLoggingRcvr);
        }
    }

    private class LoadPathsListner implements Loader.OnLoadCompleteListener<Cursor> {

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
            boolean logging = false;
            //reload the logging list
            mPathList.clear();
            //System.out.println("Load complete, number of paths are:" + data.getCount());
//            System.out.println("col_id=" + data.getInt(0));
            data.moveToFirst();
            Log.d(TAG, "col idx found is:" + data.getColumnIndex(TrafficPathsDB.COLUMN_ID));
            while (!data.isAfterLast()) {
                //loop through the rows and see if anyone needs logging
                //also update the list of route ids that need
                if (!data.isNull(data.getColumnIndex(TrafficPathsDB.COLUMN_ID))) {
                    int logEnb = data.getInt(data.getColumnIndex(TrafficPathsDB.LOGGING_ENABLED));
                    if (logEnb == 1) {
                        logging = true;
                        boolean nots = data.getInt(data.getColumnIndex(TrafficPathsDB.NOTIFICATIONS_ENABLED)) == 1;
                        String srcLatlng = data.getString(data.getColumnIndex(TrafficPathsDB.SOURCE_LATLNG));
                        String destLatlng = data.getString(data.getColumnIndex(TrafficPathsDB.DEST_LATLNG));
                        PathInfo pInfo = new PathInfo(data.getInt(data.getColumnIndex(TrafficPathsDB.COLUMN_ID)),
                                new LatLng(Double.parseDouble(srcLatlng.split(",")[0]), Double.parseDouble(srcLatlng.split(",")[1])),
                                new LatLng(Double.parseDouble(destLatlng.split(",")[0]), Double.parseDouble(destLatlng.split(",")[1])),
                                nots,
                                logging,
                                data.getString(data.getColumnIndex(TrafficPathsDB.SOURCE_ADDRESS)),
                                data.getString(data.getColumnIndex(TrafficPathsDB.DEST_ADDRESS)));
                        mPathList.add(pInfo);
                        Log.d(TAG, "Added logging path for col_id=" + pInfo.id);
                    }
                }
                data.moveToNext();
            }


            if (logging) {
                //start the recurring alarm for collecting the
                if (mIntentLogging != null) mAlarmManager.cancel(mIntentLogging);
                /*mLoggingRcvr = new LoggingAlarmReceiver();
                getApplicationContext().registerReceiver(mLoggingRcvr, new IntentFilter(LOGGING_ACTION));*/
                fireAlarmsForLogging();
            }
            else if (!logging && (mIntentLogging != null)) {
                Log.d(TAG, "Cancelling the Alarms for logging");
                //getApplicationContext().unregisterReceiver(mLoggingRcvr);
                mAlarmManager.cancel(mIntentLogging);
                mIntentLogging = null;
            }
        }
    }

    private class LoadSDNotificationListener implements Loader.OnLoadCompleteListener<Cursor>{

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
            if(DBG) Log.d(TAG, "load SD notification complete count:" + data.getCount());
            Calendar presentDate = Calendar.getInstance();
            if(DBG) Log.d(TAG, "current day=" + presentDate.get(Calendar.DAY_OF_WEEK));
            int day = dayMap[presentDate.get(Calendar.DAY_OF_WEEK)] + 1; //1st column is _id
            if(DBG) Log.d(TAG, "day:" + day);
            clearAllAlarms(DIRECTION_SD);
            data.moveToFirst();

            while(!data.isAfterLast()){
                if(!data.isNull(day)){
                    long msecs = data.getLong(day);
                    long seconds = msecs/1000;
                    int hours = (int)(seconds/3600);
                    seconds = seconds%3600;
                    int minutes = (int) seconds/60;

                    Calendar alarmCalendar = Calendar.getInstance();
                    //check against, if this time is already past
                    if((alarmCalendar.get(Calendar.HOUR_OF_DAY) > hours) ||
                            (alarmCalendar.get(Calendar.HOUR_OF_DAY) == hours && alarmCalendar.get(Calendar.MINUTE) > minutes)) {
                        data.moveToNext();
                        continue;
                    }

                    alarmCalendar.set(Calendar.HOUR_OF_DAY, hours);
                    alarmCalendar.set(Calendar.MINUTE, minutes);
                    alarmCalendar.set(Calendar.SECOND, 0);
                    alarmCalendar.set(Calendar.MILLISECOND, 0);


                    int id = data.getInt(data.getColumnIndex(NotificationsSDDB.COLUMN_ID));
                    AlarmsDay sdalarm = new AlarmsDay();
                    sdalarm.calendar = alarmCalendar;
                    sdalarm.id = id;
                    dailyAlarmsSD.add(sdalarm);

                }
                data.moveToNext();

            }
            //Fire alarms, but first clear all ongoing alarms
            fireAlarms(DIRECTION_SD);

        }


    }

    private class LoadDSNotificationListener implements Loader.OnLoadCompleteListener<Cursor>{

        @Override
        public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
            if(DBG) Log.d(TAG, "load DS notification complete count:" + data.getCount());
            Calendar presentDate = Calendar.getInstance();
            if(DBG) Log.d(TAG, "current day=" + presentDate.get(Calendar.DAY_OF_WEEK));
            int day = dayMap[presentDate.get(Calendar.DAY_OF_WEEK)] + 1; //1st column is _id
            if(DBG) Log.d(TAG, "day:" + day);
            clearAllAlarms(DIRECTION_DS);
            data.moveToFirst();

            while(!data.isAfterLast()){
                if(!data.isNull(day)){
                    long msecs = data.getLong(day);
                    long seconds = msecs/1000;
                    int hours = (int)(seconds/3600);
                    seconds = seconds%3600;
                    int minutes = (int) seconds/60;

                    Calendar alarmCalendar = Calendar.getInstance();
                    //check against, if this time is already past
                    if((alarmCalendar.get(Calendar.HOUR_OF_DAY) > hours) ||
                            (alarmCalendar.get(Calendar.HOUR_OF_DAY) == hours && alarmCalendar.get(Calendar.MINUTE) > minutes)) {
                        data.moveToNext();
                        continue;
                    }

                    alarmCalendar.set(Calendar.HOUR_OF_DAY, hours);
                    alarmCalendar.set(Calendar.MINUTE, minutes);
                    alarmCalendar.set(Calendar.SECOND, 0);
                    alarmCalendar.set(Calendar.MILLISECOND, 0);


                    int id = data.getInt(data.getColumnIndex(NotificationsDSDB.COLUMN_ID));
                    AlarmsDay dsalarm = new AlarmsDay();
                    dsalarm.calendar = alarmCalendar;
                    dsalarm.id = id;
                    dailyAlarmsDS.add(dsalarm);

                }
                data.moveToNext();

            }
            //Fire alarms, but first clear all ongoing alarms
            fireAlarms(DIRECTION_DS);

        }


    }

    private class DataBaseFeeder implements Runnable {

        private Context mContext;
        private boolean avoidH;
        private boolean avoidT;
        DataBaseFeeder(Context context, boolean avoidHighways, boolean avoidTolls) {
            mContext = context;
            avoidT = avoidTolls;
            avoidH = avoidHighways;
        }
        @Override
        public void run() {
            TrafficDurationCalc.ResourcesBing rBing;
            int durationSD, durationDS = 0;
            Calendar currCal = Calendar.getInstance();
            int hr = currCal.get(Calendar.HOUR_OF_DAY);
            int min = currCal.get(Calendar.MINUTE);
            min = LOGGING_INTERVAL * Math.round(min/LOGGING_INTERVAL);
            if (min == 60) {
                hr++;
                min = 0;
            }
            int time = hr*100 + min;

            for (PathInfo path:mPathList) {
                //DIRECTION_SD
                 rBing = TrafficDurationCalc.getDurationInTraffic(path.latlngSource,
                                                                    path.latLngDest,
                                                                    avoidH,
                                                                    avoidT);
                if (rBing == null) return;
                durationSD = rBing.durationTraffic;

                //DIRECTION_DS
                rBing = TrafficDurationCalc.getDurationInTraffic(path.latLngDest,
                        path.latlngSource,
                        avoidH,
                        avoidT);
                if (rBing == null) return;

                durationDS = rBing.durationTraffic;
                boolean carryOnLogging = true;
                carryOnLogging = DataLogger.getLogger().addData(durationSD, time, path.id, DIRECTION_SD, mContext);
                carryOnLogging = DataLogger.getLogger().addData(durationDS, time, path.id, DIRECTION_DS, mContext);
                if (!carryOnLogging) {
                    //need to turn logging off, enough data collected. Also need a notification
                    Log.d(TAG, "Turning off logging for path id " + path.id);
                    ContentValues valuesPath = new ContentValues();
                    valuesPath.put(TrafficPathsDB.LOGGING_ENABLED, false);
                    Uri uri = Uri.parse(MyContentProvider.CONTENT_URI + "/" + path.id);
                    getContentResolver().update(uri, valuesPath, null, null);
                    // This should reload the trafficpaths, recreate the list in the main context
                    //This should be all good. Just create notification now
                    notifyEndLogging(path);
                }
            }
        }

        private void notifyEndLogging(PathInfo path) {
            //setup a nice notification here
            String title = "Traffic Logs Available";
            String tickerText = "Traffic Patterns avilable from " + path.src + " to " + path.dest + " is available";

            Intent notIntent = new Intent(mContext, ChartViewer.class);
            notIntent.putExtra("SOURCE", path.src);
            notIntent.putExtra("DESTINATION", path.dest);
            notIntent.putExtra("COL_ID", path.id);
            notIntent.putExtra("DIRECTION", DIRECTION_SD);
            notIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            PendingIntent pend = PendingIntent.getActivity(mContext, 10 * path.id + DIRECTION_SD, notIntent,0); //need to rethink flag
            Notification.Builder notificationBuilder = new Notification.Builder(mContext)
                    .setContentText(tickerText)
                    .setTicker(tickerText)
                    .setContentTitle(title)
                    .setSmallIcon(R.drawable.show_chart)
                    .setContentIntent(pend).setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE);

            //the first argument below needs to be diff for all notifications for them to be displayed separately
            mNotManager.notify(NOTIFICATION_ID_LOG + 10 * path.id + DIRECTION_SD, notificationBuilder.build());
        }
    }


    // to create notifications in a worker thread because this includes
    // 1. sql query to receive data from the trafficpaths table
    //2. bing map rest access for getting time traffic for nots
    private class NotificationCreater implements Runnable{

        int mID = -1;
        int mDirection = -1;
        boolean avoidHighways = false;
        boolean avoidTolls = false;
        Context mContext;
        String[] mProjection = {TrafficPathsDB.COLUMN_ID,
            TrafficPathsDB.SOURCE_ADDRESS,
            TrafficPathsDB.DEST_ADDRESS,
            TrafficPathsDB.SOURCE_LATLNG,
            TrafficPathsDB.DEST_LATLNG,
            TrafficPathsDB.LOGGING_ENABLED,
            TrafficPathsDB.NOTIFICATIONS_ENABLED
        };
        NotificationCreater(int id, int direction, boolean avoidHighways, boolean avoidTolls,  Context context){
            mID = id;
            mContext = context;
            mDirection = direction;
            this.avoidHighways = avoidHighways;
            this.avoidTolls = avoidTolls;
        }

        @Override
        public void run() {
            Cursor cursor = getContentResolver().query(Uri.parse(MyContentProvider.CONTENT_URI + "/" + mID), mProjection, null, null, null);
            cursor.moveToFirst();//ideally we should have only one row here
            String srcName, destName, srcLatlng, destLatlng;
            LatLng sLatlng, dLatlng;

            if(cursor.getCount() == 0)
                return;

            //check if the notifications have been disabled
            int notify = cursor.getInt(cursor.getColumnIndex(TrafficPathsDB.NOTIFICATIONS_ENABLED));
            if(notify == 0) {
                if(DBG) Log.d(TAG, "Notifications disabled for this path, ID = " + mID);
                return;
            }




            if(mDirection == DIRECTION_SD) {
                srcName = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.SOURCE_ADDRESS));
                destName = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.DEST_ADDRESS));
                srcLatlng = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.SOURCE_LATLNG));
                destLatlng = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.DEST_LATLNG));
                sLatlng = new LatLng(Double.parseDouble(srcLatlng.split(",")[0]), Double.parseDouble(srcLatlng.split(",")[1]));
                dLatlng = new LatLng(Double.parseDouble(destLatlng.split(",")[0]), Double.parseDouble(destLatlng.split(",")[1]));
            }
            else{

                destName = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.SOURCE_ADDRESS));
                srcName = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.DEST_ADDRESS));
                destLatlng = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.SOURCE_LATLNG));
                srcLatlng = cursor.getString(cursor.getColumnIndex(TrafficPathsDB.DEST_LATLNG));
                dLatlng = new LatLng(Double.parseDouble(destLatlng.split(",")[0]), Double.parseDouble(destLatlng.split(",")[1]));
                sLatlng = new LatLng(Double.parseDouble(srcLatlng.split(",")[0]), Double.parseDouble(srcLatlng.split(",")[1]));
            }

            TrafficDurationCalc.ResourcesBing rBing = TrafficDurationCalc.getDurationInTraffic(sLatlng, dLatlng, avoidHighways, avoidTolls);


            if(rBing != null){
                //setup a nice notification here
                String title = "Traffic Alert";
                String tickerText = "Duration from " + srcName + " to " + destName + " in current traffic is:" + TrafficDurationCalc.getDuration(rBing.durationTraffic);
                Intent notIntent = new Intent(mContext, MapsActivity.class);
                notIntent.putExtra("SOURCE", srcName);
                notIntent.putExtra("DESTINATION", destName);
                notIntent.putExtra("LATITUDE_SOURCE", sLatlng.latitude);
                notIntent.putExtra("LATITUDE_DESTINATION", dLatlng.latitude);
                notIntent.putExtra("LONGITUDE_SOURCE", sLatlng.longitude);
                notIntent.putExtra("LONGITUDE_DESTINATION", dLatlng.longitude);
                notIntent.putExtra("AVOID_TOLLS", avoidTolls);
                notIntent.putExtra("AVOID_HIGHWAYS", avoidHighways);
                notIntent.putExtra("COL_ID", mID);
                notIntent.putExtra("DIRECTION", mDirection);
                notIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

                PendingIntent pend = PendingIntent.getActivity(mContext, 10 * mID + mDirection, notIntent,0); //need to rethink flag
                /*Notification.Builder notificationBuilder = new Notification.Builder(mContext)
                        .setContentText(tickerText)
                        .setTicker(tickerText)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.car_icon)
                        .setContentIntent(pend).setAutoCancel(true);*/

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mContext)
                                                                    .setSmallIcon(R.drawable.car_icon)
                                                                    .setContentTitle(title)
                                                                    .setContentText(TrafficDurationCalc.getDuration(rBing.durationTraffic))
                                                                    .setStyle(new NotificationCompat.BigTextStyle().bigText(tickerText))
                                                                    .setContentIntent(pend).setAutoCancel(true)
                                                                    .setDefaults(Notification.DEFAULT_SOUND|Notification.DEFAULT_VIBRATE);

                //the first argument below needs to be diff for all notifications for them to be displayed separately
                mNotManager.notify(NOTIFICATION_ID_SD + 10 * mID + mDirection, notificationBuilder.build());
            }

        }
    }
}
