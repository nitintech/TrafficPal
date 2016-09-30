package com.example.android.trafficpal;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class RouteSettings extends AppCompatActivity {
    private Button button_cancel;
    private Button button_done;
    private CheckBox box_highway, box_tolls, box_ferries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_settings);
        button_cancel = (Button) findViewById(R.id.button_cancel_setting);
        button_done = (Button) findViewById(R.id.button_done_setting);
        box_highway = (CheckBox) findViewById(R.id.checkBox_highways);
        box_tolls = (CheckBox) findViewById(R.id.checkBox_tolls);
        box_ferries = (CheckBox) findViewById(R.id.checkBox_ferries);

        Intent intent = getIntent();
        if(intent != null){
            box_highway.setChecked(intent.getBooleanExtra("Highways", false));
            box_tolls.setChecked(intent.getBooleanExtra("Tolls", false));
            box_ferries.setChecked(intent.getBooleanExtra("Ferries", false));
        }

        button_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        button_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.putExtra("Highways", box_highway.isChecked());
                intent.putExtra("Tolls", box_tolls.isChecked());
                intent.putExtra("Ferries", box_ferries.isChecked());
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_route_settings, menu);
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
