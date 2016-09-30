package com.example.android.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by niarora on 11/15/2015.
 */
public class NotificationAlarmReceiver extends BroadcastReceiver{
    private static String TAG = "NotAlarmReceiver";
    private static final int INVALID_INDEX = -1;
    int mID = INVALID_INDEX;
    int mDirection = 0;
    AlarmScheduler mScheduler = null;
    public static final int FLAG_SETUP_NOTIFICATIONS = 10;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Received alarm fire");
        if(intent != null){
            mID = intent.getIntExtra("ID", INVALID_INDEX);
            mDirection = intent.getIntExtra("DIRECTION", INVALID_INDEX);
        }
        if(mID == INVALID_INDEX || mDirection == INVALID_INDEX){
            Log.e(TAG, "Invalid index received");
            return;
        }

        /*AlarmScheduler.MyBinder binder = (AlarmScheduler.MyBinder) peekService(context, new Intent(context, AlarmScheduler.class));
        if(binder == null){
            Log.e(TAG, "Alarmscheduler service binder not available. Cannot trigger this alarm");
            return;
        }
        mScheduler = binder.getService();
        mScheduler.prepareNotification(mID);*/
        Intent serviceIntent = new Intent(context, AlarmScheduler.class);
        serviceIntent.addFlags(FLAG_SETUP_NOTIFICATIONS);
        serviceIntent.putExtra("ID", mID);
        serviceIntent.putExtra("DIRECTION", mDirection);
        context.startService(serviceIntent);

    }
}
