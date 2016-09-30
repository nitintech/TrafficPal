package com.example.android.trafficpal;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

import com.example.android.trafficpal.R;

public class DaySelection extends AppCompatActivity {

    private boolean[] m_daySelection = {false,
            true, true, true, true, true, true, true};
    private CheckBox mCheckBox[];
    private Button mButton_Ok, mButton_cancel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_selection);
        mCheckBox = new CheckBox[7];

        if (getIntent() != null) {
            Intent intent = getIntent();
            m_daySelection = intent.getBooleanArrayExtra("DAYS");
        }
        int index = 0;
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_sunday);
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_monday);
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_tuesday);
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_wednesday);
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_thursday);
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_friday);
        mCheckBox[index++] = (CheckBox) findViewById(R.id.checkBox_saturday);
        mButton_Ok = (Button) findViewById(R.id.button_done_setting_day);
        mButton_cancel = (Button) findViewById(R.id.button_cancel_setting_day);

        for (int i = 0; i < 7; i++) { //index 0 is Sunday
            mCheckBox[i].setChecked(m_daySelection[i+1]);
        }

        mButton_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        mButton_Ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                for (int i = 0; i < 7; i++) {
                    m_daySelection[i+1] = mCheckBox[i].isChecked();
                }
                intent.putExtra("DAYSELECTION", m_daySelection);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_day_selection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
