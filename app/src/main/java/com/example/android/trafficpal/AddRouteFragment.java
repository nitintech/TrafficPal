package com.example.android.trafficpal;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.location.places.GeoDataApi;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;

import java.util.HashMap;
import java.util.Map;

/**
 * A placeholder fragment containing a simple view.
 */
public class AddRouteFragment extends Fragment {

    private static final String TAG = "Fragment.AddRouteFrag";
    SetRouteCallbacks mParent;
    Button btn_sd, btn_ds, button_save;
    AutoCompleteTextView txtview_source, txtview_dest;
    CheckBox box_logging, box_notify;
    PlacesAutoCompleteAdapter mAdapter;
    String mSrcAddr, mDestAddr;
    boolean mIsNotify, mIsLogged;
    public static final boolean DBG = false;


    public AddRouteFragment() {
    }

    public void setPlacedAdapter(PlacesAutoCompleteAdapter adapter){
        mAdapter = adapter;
        txtview_source.setAdapter(mAdapter);
        txtview_source.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String address = mAdapter.getItem(position);
                String placeId = mAdapter.addressMap.get(address);
                if (placeId != null) {
                    //Pass it to the activity
                    mParent.setPlaceIdSrc(placeId);
                }
                txtview_source.setText(mAdapter.getItem(position));
            }
        });

        txtview_dest.setAdapter(mAdapter);
        txtview_dest.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String address = mAdapter.getItem(position);
                String placeId = mAdapter.addressMap.get(address);
                if (placeId != null) {
                    //Pass it to the activity
                    mParent.setPlaceIdDest(placeId);
                }
                txtview_dest.setText(mAdapter.getItem(position));
            }
        });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_route, container, false);
        if(DBG) Log.d(TAG, "onCreateView");

        txtview_source = (AutoCompleteTextView) rootView.findViewById(R.id.editText_source);
        txtview_dest = (AutoCompleteTextView) rootView.findViewById(R.id.editText_dest);

        if(mAdapter != null){
            txtview_source.setAdapter(mAdapter);
            txtview_dest.setAdapter(mAdapter);
        }
        box_logging = (CheckBox) rootView.findViewById(R.id.checkBox_logging);
        box_notify = (CheckBox) rootView.findViewById(R.id.checkBox_notifications);
        //initialize the buttons
        btn_sd = (Button) rootView.findViewById(R.id.button_nots_to);
        btn_ds = (Button) rootView.findViewById(R.id.button_nots_back);
        button_save = (Button) rootView.findViewById(R.id.button_save_path);
        if(btn_sd != null){
            btn_sd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mParent.onNotificationsButton(1);
                }
            });
        }
        if(btn_ds != null){
            btn_ds.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mParent.onNotificationsButton(2);
                }
            });
        }
        if(button_save != null){
            button_save.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mParent.onSavePath(txtview_source.getText().toString(),
                            txtview_dest.getText().toString(),
                            box_logging.isChecked(),
                            box_notify.isChecked());
                }
            });
        }

        //set Initial values if present
        if(mSrcAddr != null && !mSrcAddr.equals("")) {
            txtview_source.setText(mSrcAddr);
            txtview_source.setEnabled(false);
            //txtview_source.setHorizontallyScrolling(false);
            //txtview_source.setSingleLine(false);
        }
        if(mDestAddr != null && !mDestAddr.equals("")){
            txtview_dest.setText(mDestAddr);
            txtview_dest.setEnabled(false);
            //txtview_dest.setHorizontallyScrolling(false);
            //txtview_dest.setSingleLine(false);
        }
        if(mIsNotify) {
            enableNotsCheck(true);
            box_notify.setChecked(mIsNotify);
        }
        if(mIsLogged) {
            box_logging.setChecked(mIsLogged);
        }
        return rootView;
    }

    @Override
    public void onAttach(Activity activity) {
        if(DBG) Log.d(TAG, "onAttach");
        mParent = (SetRouteCallbacks) activity;
        super.onAttach(activity);
    }

    @Override
    public void onDestroy() {
        if(DBG) Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public interface SetRouteCallbacks{
        public void onNotificationsButton(int direction);
        public void onSavePath(String source, String dest, boolean log, boolean not);
        public void setPlaceIdSrc(String src);
        public void setPlaceIdDest(String dest);
    }

    public void initValues(String src, String dest, boolean isNotify, boolean isLogged){
        mSrcAddr = src;
        mDestAddr = dest;
        mIsNotify = isNotify;
        mIsLogged = isLogged;
    }

    public void enableNotsCheck(boolean enable){
        box_notify.setEnabled(enable);
        if(!enable)
            box_notify.setChecked(enable);
    }
}
