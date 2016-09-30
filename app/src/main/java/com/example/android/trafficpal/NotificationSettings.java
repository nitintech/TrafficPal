package com.example.android.trafficpal;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.v7.internal.widget.ViewStubCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;

import org.w3c.dom.Text;

import java.io.Serializable;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NotificationSettings.OnFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class NotificationSettings extends Fragment{

    private OnFragmentInteractionListener mListener;
    private NotificationTimings mNotTimings;
    private TimePicker timePicker;
    private Button[] buttons = new Button[7];
    private int selectedButton = -1;
    Drawable default_button_color;
    private static final String TAG = "Fragment.NotificationSettings";
    private TextView hint_TextView;
    private View mRootView;

    public void setNotificationTimings(NotificationTimings refTimings){
        mNotTimings = refTimings;
    }

    public NotificationSettings() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_notification_settings, container, false);
        //find the timepicker
        timePicker = (TimePicker) rootView.findViewById(R.id.timePicker);
        hint_TextView = (TextView) rootView.findViewById(R.id.textview_hints);
        initializeButtons(rootView);
        SetTimePicker(1);
        mRootView = rootView;
        return rootView;
    }

    @Override
    public void onResume() {
        //set the date picker and button colors based on the notificationtiming object
        selectedButton = -1;
        hint_TextView.setText(getResources().getText(R.string.hint_select));

        if(mNotTimings != null) {

            for (int i = 0; i < 7; i++) {
                if (mNotTimings.isEnabled(i)) {
                    timePicker.setCurrentHour(mNotTimings.getHourForDay(i));
                    timePicker.setCurrentMinute(mNotTimings.getMinForDay(i));
                    buttons[i].setBackground(new ColorDrawable(getResources().getColor(R.color.orange)));
                }
            }
        }
        super.onResume();
    }

    private void initializeButtons(View view){
        buttons[0] = (Button) view.findViewById(R.id.button_mon);
        buttons[1] = (Button) view.findViewById(R.id.button_tue);
        buttons[2] = (Button) view.findViewById(R.id.button_wed);
        buttons[3] = (Button) view.findViewById(R.id.button_thur);
        buttons[4] = (Button) view.findViewById(R.id.button_fri);
        buttons[5] = (Button) view.findViewById(R.id.button_sat);
        buttons[6] = (Button) view.findViewById(R.id.button_sun);
        default_button_color = buttons[0].getBackground();
        for(int i = 0; i < 7; i++){
            final int index = i;
            buttons[i].setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    buttonClickListeners(index);
                }
            });
        }
    }
    private void buttonClickListeners(int day){
        if(day == selectedButton){
            //enable/disable the day
            if(mNotTimings.isEnabled(day)){
                //disable it
                mNotTimings.setEnabled(day, false);
                buttons[day].setBackground(new ColorDrawable(getResources().getColor(R.color.blue)));
            }
            else{
                //enable it
                mNotTimings.setEnabled(day, true);
                buttons[day].setBackground(new ColorDrawable(getResources().getColor(R.color.orange)));
                //save it
                int hourpicker = timePicker.getCurrentHour();
                int minPicker = timePicker.getCurrentMinute();
                mNotTimings.setHourMin(selectedButton, hourpicker,minPicker);
            }
        }
        else{
            //unselect the day previously selected
            if(selectedButton>=0 && !mNotTimings.isEnabled(selectedButton)){ //last selection was not saved
                buttons[selectedButton].setBackground(default_button_color);
            }
            else if(selectedButton>=0 && mNotTimings.isEnabled(selectedButton)){ //save the last selection
                /*
                int hourpicker = timePicker.getCurrentHour();
                int minPicker = timePicker.getCurrentMinute();
                mNotTimings.setHourMin(selectedButton, hourpicker,minPicker);*/
                buttons[selectedButton].setBackground(new ColorDrawable(getResources().getColor(R.color.orange)));
            }
            //select that day and show its corresponding time
            //timePicker.invalidate();
            //timePicker.setIs24HourView(false);
            timePicker.clearFocus();
            int hrDay = mNotTimings.getHourForDay(day);
            int minDay = mNotTimings.getMinForDay(day);
            //((TimePicker) mRootView.findViewById(R.id.timePicker)).setHour(hrDay);
            //((TimePicker) mRootView.findViewById(R.id.timePicker)).setMinute(minDay);
            timePicker.setCurrentHour(new Integer(hrDay));
            timePicker.setCurrentMinute(new Integer(minDay));

            buttons[day].setBackground(new ColorDrawable(getResources().getColor(R.color.blue)));
            selectedButton = day;

        }
        //show hints
        if(mNotTimings.isEnabled(day))
            showHintEnable(false);
        else
            showHintEnable(true);
    }

    private void showHintEnable(boolean enable){
        if(enable)
            hint_TextView.setText(getResources().getText(R.string.hint_enable));
        else
            hint_TextView.setText(getResources().getText(R.string.hint_disable));
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }

    private void SetTimePicker(int c){

    }

}
