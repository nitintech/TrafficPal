package com.example.android.trafficpal;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.android.database.DataLogger;
import com.example.android.notifications.AlarmScheduler;
import com.example.android.trafficpal.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.AxisValueFormatter;
import com.github.mikephil.charting.highlight.ChartHighlighter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.Utils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ChartViewer extends AppCompatActivity implements OnChartValueSelectedListener {
    private LineChart lineChart;
    private DataLogger mLogger;
    private TextView mTextViewSrc, mTextViewDest, mTextViewHigh;
    private  String mSrc, mDest;
    private int m_id, mDirection;
    private ImageButton mBtnReverse;
    private static final String TAG = "ChartViewer";
    private LineDataSet[] lineSets;
    private LineDataSet[] lineSets_rev;
    private boolean bRevCal = false;
    private boolean bFwdCal = false;
    private static String[] dayDesc = {"blah",
            "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"};
    private static final int[] colors = {0,
            Color.BLACK, Color.BLUE, Color.RED, Color.GREEN, Color.CYAN, Color.YELLOW, Color.MAGENTA};
    private ArrayList<ILineDataSet> m_dataSets;
    private ArrayList<ILineDataSet> m_dataSets_rev;
    private LineData mLineData;
    private boolean[] daySelection = {false,
            true, true, true, true, true, true, true};
    private static int REQUEST_CODE_DAY = 10;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            daySelection = savedInstanceState.getBooleanArray("DAYS");
        }
        if(getSupportActionBar() == null)
            Log.e(TAG, "Action bar is null");
        else
            GenericFunctions.setUpActionBarColor(this, getResources().getColor(R.color.color_pink));
        mLogger = DataLogger.getLogger();

        setContentView(R.layout.activity_chart_viewer);
        lineChart = (LineChart) findViewById(R.id.chart);
        mTextViewSrc = (TextView) findViewById(R.id.Text_chartsource);
        mTextViewDest = (TextView) findViewById(R.id.Text_chartdest);
        mTextViewHigh = (TextView) findViewById(R.id.Text_hightlighted);
        mBtnReverse = (ImageButton) findViewById(R.id.button_chartreverse);

        lineSets = new LineDataSet[8];
        lineSets_rev = new LineDataSet[8];

        m_dataSets = new ArrayList<ILineDataSet>();
        m_dataSets_rev = new ArrayList<>();
        //all u need to do is now add and remove sets from m_dataSets.. I guess!!

        mSrc = getIntent().getStringExtra("SOURCE");
        mDest = getIntent().getStringExtra("DESTINATION");
        m_id = getIntent().getIntExtra("COL_ID", -1);
        mDirection = getIntent().getIntExtra("DIRECTION", 0);
        if(mSrc != null && mDest!= null){
            String srcHtml = "<b>SOURCE: </b>"+ "<font color = \"blue\">" + mSrc + "</font>";
            mTextViewSrc.setText(Html.fromHtml(srcHtml));
            String destHtml = "<b>DESTINATION: </b>"+ "<font color = \"blue\">" + mDest+ "</font>";
            mTextViewDest.setText(Html.fromHtml(destHtml));
        }

        //initialize data sets.. This shld be done ideally on a separate worker thread
        if (mDirection == AlarmScheduler.DIRECTION_DS) {
            initiateRevDataSets();
            mLineData = new LineData(m_dataSets_rev);
        }
        else {
            initiateDataSets();
            mLineData = new LineData(m_dataSets);
        }
        //initiateRevDataSets();

        lineChart.setData(mLineData);

        mBtnReverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirection = (mDirection == 0) ? 1 : 0;
                String temp = mSrc;
                mSrc = mDest;
                mDest = temp;
                if (mSrc != null && mDest != null) {
                    String srcHtml = "<b>SOURCE: </b>" + "<font color = \"blue\">" + mSrc + "</font>";
                    mTextViewSrc.setText(Html.fromHtml(srcHtml));
                    String destHtml = "<b>DESTINATION: </b>" + "<font color = \"blue\">" + mDest + "</font>";
                    mTextViewDest.setText(Html.fromHtml(destHtml));
                }
                if (mDirection == AlarmScheduler.DIRECTION_SD) {
                    if (!bFwdCal) initiateDataSets();
                    mLineData = new LineData(m_dataSets);
                }
                else {
                    if (!bRevCal) initiateRevDataSets();
                    mLineData = new LineData(m_dataSets_rev);
                }
                lineChart.setData(mLineData);
                lineChart.getData().notifyDataChanged();
                lineChart.notifyDataSetChanged();
                //setData();
                lineChart.invalidate();
            }
        });

        setChartProperties();
        setXAxis();
        setYAxis();
        //setLimitLines();
        lineChart.getData().notifyDataChanged();
        lineChart.notifyDataSetChanged();
        //setData();
        lineChart.invalidate();
    }

    private void setLimitLines() {
        Calendar c = Calendar.getInstance();

        int hr = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);
        int labelVal = hr*100 + min;

        LimitLine llXAxis = new LimitLine(labelVal, "Current Time " + hr + ":" + min);
        llXAxis.setLineWidth(4f);
        llXAxis.enableDashedLine(10f, 10f, 0f);
        llXAxis.setLabelPosition(LimitLine.LimitLabelPosition.RIGHT_TOP);
        llXAxis.setTextSize(10f);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.addLimitLine(llXAxis);
    }

    private void setChartProperties(){
        lineChart.setDescription("-----------------------------------------> (Time of Day)");
        lineChart.setDescriptionTextSize(15f);
        lineChart.setTouchEnabled(true);
        Legend l = lineChart.getLegend();
        l.setPosition(Legend.LegendPosition.RIGHT_OF_CHART_INSIDE);
        l.setXEntrySpace(7f);
        l.setYEntrySpace(0f);
        l.setYOffset(0f);
        lineChart.animateX(2000);
        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        lineChart.setOnChartValueSelectedListener(this);
        //lineChart.setHighlightPerDragEnabled(true);
        //lineChart.setHighlightPerDragEnabled(true);
    }

    private void setXAxis() {
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setDrawGridLines(true);
        xAxis.setLabelCount(12);
        xAxis.setValueFormatter(new TimeAxisValueFormatter());
    }

    private void setYAxis() {
        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setPosition(YAxis.YAxisLabelPosition.INSIDE_CHART);
        yAxis.setDrawGridLines(true);
        lineChart.getAxisRight().setEnabled(false);
        yAxis.setTextColor(Color.BLACK);
    }

    @Override
    public void onValueSelected(Entry e, Highlight h) {

        float timeSec = h.getX();
        int timeHr = (int)timeSec;
        int timeMin = (int)((timeSec-timeHr)*60);
        int durationSec = (int) (h.getY()*60);

        StringBuilder builder = new StringBuilder();
        builder.append("<b>TIME:</b>");
        builder.append(timeHr).append(":").append(timeMin);

        builder.append(" <b>DURATION:</b>");
        int hours = durationSec/3600;
        durationSec = durationSec%3600;
        int minutes = durationSec/60;
        if (hours > 0) {
            builder.append(hours).append(" hrs ");
        }
        if (minutes > 0) {
            builder.append(minutes).append(" min");
        }

        mTextViewHigh.setText(Html.fromHtml(builder.toString()));
    }

    @Override
    public void onNothingSelected() {

    }

    private class TimeAxisValueFormatter implements AxisValueFormatter {

        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            StringBuilder builder = new StringBuilder();
            int hrs = (int) value;
            int min =(int) ((value - hrs)*60);
            builder.append(hrs).append(":").append(min);
            return builder.toString();
        }

        @Override
        public int getDecimalDigits() {
            return 0;
        }
    }

    private void initiateDataSets() {
        bFwdCal = true;
        for (int day = 1; day <= 7; day++) {
            ArrayList<Entry> values = new ArrayList<Entry>();
            Map<Integer,Integer> map =  mLogger.selectDataForDay(m_id, AlarmScheduler.DIRECTION_SD, this, day);
            if (map == null || map.size() == 0) continue;
            for (int i = 0; i <2400; i+= AlarmScheduler.LOGGING_INTERVAL) {
                if (map.containsKey(i)) {
                    int hr = i/100;
                    int min = i%100;
                    values.add(new Entry(hr+(float)min/60, map.get(i) / (float) 60));
                }
            }
            lineSets[day] = new LineDataSet(values, dayDesc[day]);
            lineSets[day].setHighLightColor(Color.GREEN);
            lineSets[day].setColor(colors[day]);
            lineSets[day].setCircleColor(colors[day]);
            lineSets[day].setLineWidth(2f);
            lineSets[day].setCircleRadius(3f);
            lineSets[day].setDrawCircleHole(false);
            lineSets[day].setValueTextSize(9f);
            lineSets[day].setHighlightEnabled(true);
            lineSets[day].setDrawValues(false);

            if (daySelection[day]) m_dataSets.add(lineSets[day]);
        }
    }

    private void initiateRevDataSets() {
        bRevCal = true;
        for (int day = 1; day <= 7; day++) {
            ArrayList<Entry> values = new ArrayList<Entry>();
            Map<Integer,Integer> map =  mLogger.selectDataForDay(m_id, AlarmScheduler.DIRECTION_DS, this, day);
            if (map == null || map.size() == 0) continue;
            for (int i = 0; i <2400; i+= AlarmScheduler.LOGGING_INTERVAL) {
                if (map.containsKey(i)) {
                    values.add(new Entry(i, map.get(i) / (float) 60));
                }
            }
            lineSets_rev[day] = new LineDataSet(values, dayDesc[day]);
            lineSets_rev[day].setHighLightColor(Color.GREEN);
            lineSets_rev[day].setColor(colors[day]);
            lineSets_rev[day].setCircleColor(colors[day]);
            lineSets_rev[day].setLineWidth(2f);
            lineSets_rev[day].setCircleRadius(3f);
            lineSets_rev[day].setDrawCircleHole(false);
            lineSets_rev[day].setValueTextSize(9f);
            lineSets_rev[day].setHighlightEnabled(true);
            lineSets_rev[day].setDrawValues(false);
            if (daySelection[day]) m_dataSets_rev.add(lineSets_rev[day]);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chart_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_options) {
            Intent dayIntent = new Intent(ChartViewer.this, DaySelection.class);
            dayIntent.putExtra("DAYS", daySelection);
            startActivityForResult(dayIntent, REQUEST_CODE_DAY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBooleanArray("DAYS", daySelection);
        super.onSaveInstanceState(outState);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DAY && resultCode == RESULT_OK) {
            if (data != null) {
                daySelection = data.getBooleanArrayExtra("DAYSELECTION");

                if (!bFwdCal) initiateDataSets();
                if (!bRevCal) initiateRevDataSets();

                m_dataSets_rev.clear();
                m_dataSets.clear();
                for (int i = 1; i <= 7; i++) {
                    if (daySelection[i]) {
                        if (lineSets[i] != null)
                            m_dataSets.add(lineSets[i]);
                        if (lineSets_rev[i] != null)
                            m_dataSets_rev.add(lineSets_rev[i]);
                    }
                }
                if (mDirection == AlarmScheduler.DIRECTION_SD) {
                    mLineData = new LineData(m_dataSets);
                }
                else {
                    mLineData = new LineData(m_dataSets_rev);
                }
                lineChart.setData(mLineData);
                lineChart.getData().notifyDataChanged();
                lineChart.notifyDataSetChanged();
                lineChart.invalidate();
            }
        }
    }
}
