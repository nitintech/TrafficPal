package com.example.android.trafficpal;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddRoute extends AppCompatActivity implements AddRouteFragment.SetRouteCallbacks,
        NotificationSettings.OnFragmentInteractionListener, GoogleApiClient.OnConnectionFailedListener,GoogleApiClient.ConnectionCallbacks {

    private FrameLayout mFrameSettings, mFrameNotifications;
    private  AddRouteFragment mSettingsFragment = new AddRouteFragment();
    private  NotificationSettings mNotificationFragment = new NotificationSettings();
    private static final String TAG = "Activity.AddRoute";
    private final FragmentManager mFragmentManager = getFragmentManager();
    private NotificationTimings mNotSD, mNotDS;
    private GoogleApiClient mGoogleClient;
    private LatLngBounds mBounds = new LatLngBounds(new LatLng(32.67, -117.24), new LatLng(44.929754, -67.358041));
    private AutocompleteFilter mCompleteFilter = AutocompleteFilter.create(new ArrayList<Integer>(Place.TYPE_STREET_ADDRESS));
    private PlacesAutoCompleteAdapter mPlacesAdapter;
    private LatLng mSrcLatlng, mDestLatlng;
    private int mDirection = 0;
    private boolean isEditMode = false;
    private int mEditIndex = -1;
    private static final boolean DBG = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //TODO do actions if configuration changes.
        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                setupInterface();
            }
        });
        if(getIntent() != null){
            //initialize fragments based on input data
            initializeFragments(getIntent());
            isEditMode = true;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_route);
        //Initialize controls
        mFrameSettings = (FrameLayout) findViewById(R.id.RouteOptions);
        mFrameNotifications = (FrameLayout) findViewById(R.id.NotificationSettings);

        if(getSupportActionBar() == null)
            Log.e(TAG, "Action bar is null");
        else {
            GenericFunctions.setUpActionBarColor(this, getResources().getColor(R.color.color_pink));
        }
        //Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        //setSupportActionBar(toolbar);
        //toolbar.setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.color_blue)));

        //Now set the NotificationSettings objects for morning and Evening based on whether its a new data or not
        //creating new notificationSettings assuming new path to be added
        if(savedInstanceState != null){
            mNotSD = savedInstanceState.getParcelable("NotSD");
            mNotDS = savedInstanceState.getParcelable("NotDS");
        }else if(mNotSD == null && mNotDS == null){
            mNotSD = new NotificationTimings(NotificationTimings.DIRECTION_SD);
            mNotDS = new NotificationTimings(NotificationTimings.DIRECTION_DS);
        }

        //create a connection to the places auto complete
        mGoogleClient = new GoogleApiClient.Builder(this)
                        //.enableAutoManage(this, 0, this)
                        .addApi(Places.GEO_DATA_API)
                        .addConnectionCallbacks(this)
                        .build();

        if(savedInstanceState != null){
            mSettingsFragment = (AddRouteFragment) mFragmentManager.findFragmentById(R.id.RouteOptions);
            int direction = savedInstanceState.getInt("NotificationDirection", -1);
            if(direction == 1){
                //maybe do something for the second guy here.
                mNotificationFragment = (NotificationSettings) mFragmentManager.findFragmentById(R.id.NotificationSettings);
                mNotificationFragment.setNotificationTimings(mNotSD);
                mDirection = direction;
            }
            else if(direction == 2){
                //maybe do something for the second guy here.
                mNotificationFragment = (NotificationSettings) mFragmentManager.findFragmentById(R.id.NotificationSettings);
                mNotificationFragment.setNotificationTimings(mNotDS);
                mDirection = direction;
            }
            if(getIntent()!=null && (getIntent().getIntExtra("Index", -1) != -1))
                initializeFragments(getIntent());
        }
        else {

            getFragmentManager().beginTransaction()
                    .add(R.id.RouteOptions, mSettingsFragment)
                    .commit();
        }
    }

    void initializeFragments(Intent intent){
        mNotSD = intent.getParcelableExtra("NotSD");
        mNotDS = intent.getParcelableExtra("NotDS");
        mSrcLatlng = intent.getParcelableExtra("SrcLatLng");
        mDestLatlng = intent.getParcelableExtra("DestLatLng");
        mSettingsFragment.initValues(intent.getStringExtra("SourceAddress"),
                intent.getStringExtra("DestAddress"),
                intent.getBooleanExtra("IsNotify", false),
                intent.getBooleanExtra("isLogging", false));
        mEditIndex = intent.getIntExtra("Index", -1);
    }

    private void enableNotificationCheck(){
        //enable / disable the checkbox in route settings for nots
        boolean enable = false;
        for(int i = 0; i < 7; i++){
            if(mNotSD.isEnabled(i) || mNotDS.isEnabled(i)){
                enable = true;
                break;
            }
        }
        if(enable){
            mSettingsFragment.enableNotsCheck(true);
        }else{
            mSettingsFragment.enableNotsCheck(false);
        }
    }

    private void setupInterface(){
        if(!mNotificationFragment.isAdded()){
            // settings frame takes the full width
            mFrameSettings.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            mFrameNotifications.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            //at this point enable/disable the checkbox for notifications
            enableNotificationCheck();
            mDirection = -1;
        }
        else{
            mFrameSettings.setLayoutParams(new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.MATCH_PARENT
            ));
            mFrameNotifications.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            ));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_route, menu);
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

    @Override
    public void onNotificationsButton(int direction) {
        //make the notification fragment visible here
        if(direction ==1){
            mNotificationFragment.setNotificationTimings(mNotSD);
        }
        else if(direction == 2){
            mNotificationFragment.setNotificationTimings(mNotDS);
        }
        if(!mNotificationFragment.isAdded()){
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.add(R.id.NotificationSettings,mNotificationFragment);
            transaction.addToBackStack(null);
            transaction.commit();
            mFragmentManager.executePendingTransactions();
        }
        mDirection = direction;
    }

    @Override
    public void onSavePath(String source, String dest, boolean log, boolean not) {
        Intent returnIntent = new Intent();
        Parcelable[] notTimings = new Parcelable[2];
        notTimings[0] = mNotSD;
        notTimings[1] = mNotDS;
        if(!validateUserInputs(source, dest)){
            //show error message here
            Toast.makeText(this, "Invalid input address", Toast.LENGTH_SHORT).show();
            return;
        }
        returnIntent.putExtra("Notifications", notTimings);
        returnIntent.putExtra("Source", source);
        returnIntent.putExtra("Destination", dest);
        returnIntent.putExtra("Logging", log);
        returnIntent.putExtra("Notify", not);
        //get lat/long, else notify user
        LatLng srcLatLng, destLatLng;
        if(mSrcLatlng != null)
            srcLatLng = mSrcLatlng;
        else
            srcLatLng = getLatLngs(source);

        if(mDestLatlng != null)
            destLatLng = mDestLatlng;
        else
            destLatLng = getLatLngs(dest);

        if(srcLatLng == null || destLatLng == null){
            Toast.makeText(this, "Invalid address or Network error", Toast.LENGTH_SHORT).show();
            return;
        }
        returnIntent.putExtra("srclat", srcLatLng.latitude);
        returnIntent.putExtra("srclong", srcLatLng.longitude);
        returnIntent.putExtra("destlat", destLatLng.latitude);
        returnIntent.putExtra("destlong", destLatLng.longitude);
        returnIntent.putExtra("Index", mEditIndex);

        setResult(RESULT_OK, returnIntent);
        finish();
    }

    @Override
    public void setPlaceIdSrc(String src) {
        //get the Latlngs based on the place ID using synchronous call
        mSrcLatlng = null;
        if(src != null) {
            Places.GeoDataApi.getPlaceById(mGoogleClient, src)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                        @Override
                        public void onResult(PlaceBuffer places) {
                            if(places != null && places.getStatus().isSuccess()){
                                Place place = places.get(0);
                                if(place != null){
                                    mSrcLatlng = place.getLatLng();
                                }

                            }
                            places.release();
                        }
                    });
        }
    }

    @Override
    public void setPlaceIdDest(String dest) {
        //get the Latlngs based on the place ID using synchronous call
        mDestLatlng = null;
        if(dest != null) {
            Places.GeoDataApi.getPlaceById(mGoogleClient, dest)
                    .setResultCallback(new ResultCallback<PlaceBuffer>() {
                        @Override
                        public void onResult(PlaceBuffer places) {
                            if(places != null && places.getStatus().isSuccess()){
                                Place place = places.get(0);
                                if(place != null){
                                    mDestLatlng = place.getLatLng();
                                }

                            }
                            places.release();
                        }
                    });
        }
    }

    private LatLng getLatLngs(String addressStr){
        Geocoder geocoder = new Geocoder(this);
        if(!Geocoder.isPresent())
            return null;
        try {
            List<Address> addressListSrc = geocoder.getFromLocationName(addressStr, 5);
            if(addressListSrc == null || addressListSrc.size() == 0) {
                //try a bit harder for a multi part addresses, look after the coma
                String[] lStringSet = addressStr.split(",",2);
                if(lStringSet != null && lStringSet.length > 1){
                    addressListSrc = geocoder.getFromLocationName(lStringSet[1], 5);
                    if(DBG) Log.d(TAG, "addressList :" + addressListSrc + " size:" + addressListSrc.size());
                    if(addressListSrc.size() == 0) return null;
                }
                else
                    return null;
            }
            return new LatLng(addressListSrc.get(0).getLatitude(), addressListSrc.get(0).getLongitude());
        }
        catch(IllegalArgumentException e){
            Log.e(TAG, "Incorrect arguement for calculating latlng");
            return null;
        }
        catch(IOException e){
            Log.e(TAG,"geocoder results not available");
            return null;
        }
    }

    private boolean validateUserInputs(String src, String dest){
        //check the validity of addresses here TODO
        if(src.trim().compareTo("") == 0 || dest.trim().compareTo("") == 0)
            return false;
        else
            return true;
    }

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    @Override
    public void onBackPressed() {
        if(mFragmentManager.getBackStackEntryCount() > 0){
            mFragmentManager.popBackStack();
        }else{
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //if(mNotificationFragment!= null && mNotificationFragment.isAdded()) {
        outState.putInt("NotificationDirection", mDirection);
        outState.putParcelable("NotSD", mNotSD);
        outState.putParcelable("NotDS", mNotDS);
        //}
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        setupInterface();
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mGoogleClient != null)
            mGoogleClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(mGoogleClient != null)
            mGoogleClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Error connecting to Google API");
    }

    @Override
    public void onConnected(Bundle bundle) {
        if(DBG) Log.d(TAG, "GoogleAPIClient is connected");
        if(mGoogleClient != null && mGoogleClient.isConnected()) {
            mPlacesAdapter = new PlacesAutoCompleteAdapter(this, mGoogleClient, mBounds, null);
            mSettingsFragment.setPlacedAdapter(mPlacesAdapter);
        }
        else {
            Log.e(TAG,"service not connected");
            mPlacesAdapter = null;
            mSettingsFragment.setPlacedAdapter(mPlacesAdapter);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.e(TAG, "connection suspended:" + i);
        mPlacesAdapter = null;
        mSettingsFragment.setPlacedAdapter(null);
    }
}
