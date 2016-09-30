package com.example.android.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Created by niarora on 7/27/2016.
 */
public class LoggingAlarmReceiver extends BroadcastReceiver {
    public static final int FLAG_START_LOGGING = 1000;
    private static final String TAG = "LoggingAlarmReceiver";
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        context.unregisterReceiver(this);
        Intent serviceIntent = new Intent(context, AlarmScheduler.class);
        serviceIntent.addFlags(FLAG_START_LOGGING);
        context.startService(serviceIntent);
    }
}
