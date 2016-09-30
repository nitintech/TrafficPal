package com.example.android.trafficpal;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by niarora on 10/1/2015.
 */
public class NotificationTimings implements Parcelable{

    public static final int MONDAY = 0;
    public static final int TUESDAY = 1;
    public static final int WEDNESDAY = 2;
    public static final int THURSDAY = 3;
    public static final int FRIDAY = 4;
    public static final int SATURDAY = 5;
    public static final int SUNDAY = 6;

    public static final int DIRECTION_SD = 0;
    public static final int DIRECTION_DS = 1;

    private static final int MORNING_HOUR = 7;
    private static final int MORNING_MIN = 0;

    private static final int EVENING_HOUR = 17;
    private static final int EVENING_MIN = 0;

    DayAndTime[] dayTime = new DayAndTime[7];

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        for(int i = 0; i < 7; i++){
            dest.writeParcelable(dayTime[i], flags);
        }
    }

    public static final Creator CREATOR = new Creator() {
        @Override
        public Object createFromParcel(Parcel source) {
            NotificationTimings nt = new NotificationTimings(0);
            /*nt.dayTime = (DayAndTime[]) source.readParcelableArray(NotificationTimings.class.getClassLoader());*/
            for(int i=0; i<7; i++){
                nt.dayTime[i] = source.readParcelable(NotificationTimings.class.getClassLoader());
            }
            return nt;
        }

        @Override
        public Object[] newArray(int size) {
            return new NotificationTimings[size];
        }
    };

    public static class DayAndTime implements Parcelable{
        int hour;
        int min;
        long timeMillis;
        boolean enabled;
        private DayAndTime(int lHour, int lMin, long lTime){
            hour = lHour;
            min = lMin;
            timeMillis = lTime;
            enabled = false;
        }

        public DayAndTime(Parcel in){
            hour = in.readInt();
            min = in.readInt();
            timeMillis = in.readLong();
            enabled = (in.readByte() == 1?true:false);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(hour);
            dest.writeInt(min);
            dest.writeLong(timeMillis);
            dest.writeByte((byte)(enabled?1:0));
        }

        public static final Creator CREATOR = new Creator() {
            @Override
            public Object createFromParcel(Parcel source) {
                /*DayAndTime dt = new DayAndTime(source.readInt(), source.readInt(), source.readInt());
                dt.enabled = (boolean) (source.readByte() == 1?true:false);
                return dt;*/
                return new DayAndTime(source);
            }

            @Override
            public Object[] newArray(int size) {
                return new DayAndTime[size];
            }
        };
    }

    public long convertToMillis(int hour, int min, int sec){
        return ((hour*60*60 + min*60 + sec)*1000);
    }

    //constructor based on Direction (S->D or D->S)
    public NotificationTimings(int direction) {
        //initialize timings based on direction
        long setTime;
        int lHour, lMin;
        if (direction == DIRECTION_SD) { //fill with morning 7am
            setTime = convertToMillis(MORNING_HOUR, MORNING_MIN,0);
            lHour = MORNING_HOUR;
            lMin = MORNING_MIN;

        }
        else{
            setTime = convertToMillis(EVENING_HOUR, EVENING_MIN,0);
            lHour = EVENING_HOUR;
            lMin = EVENING_MIN;
        }
        for(int i = 0; i < 7; i++){
            dayTime[i] = new DayAndTime(lHour, lMin, setTime);
        }
    }

    public int getHourForDay(int day){
        return dayTime[day].hour;
    }
    public int getMinForDay(int day){
        return dayTime[day].min;
    }
    public long getMillis(int day) {return dayTime[day].timeMillis;}

    public boolean isEnabled(int day){
        return dayTime[day].enabled;
    }
    public void setEnabled(int day, boolean enable){
        dayTime[day].enabled = enable;
    }

    public void setHourMin(int day, int hour, int min){
        dayTime[day].hour = hour;
        dayTime[day].min = min;
        dayTime[day].timeMillis = convertToMillis(hour, min,0);
    }

    public void setMillis(int day, long millis){
        long seconds = millis/1000;
        int hours = (int)(seconds/3600);
        seconds = seconds%3600;
        int minutes = (int) seconds/60;

        dayTime[day].hour = hours;
        dayTime[day].min = minutes;
        dayTime[day].timeMillis = millis;
    }

}
