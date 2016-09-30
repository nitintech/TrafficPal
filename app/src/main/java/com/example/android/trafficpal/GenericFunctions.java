package com.example.android.trafficpal;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by niarora on 9/28/2015.
 */
class GenericFunctions {
    public static void setUpActionBarColor(AppCompatActivity context, int color){
        context.getSupportActionBar().setBackgroundDrawable(new ColorDrawable(color));
    }

    public static void setUpActionBarColor(Activity context, int color){
        context.getActionBar().setBackgroundDrawable(new ColorDrawable(color));
    }
}
