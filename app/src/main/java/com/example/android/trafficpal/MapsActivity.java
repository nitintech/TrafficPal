package com.example.android.trafficpal;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.database.DataLogger;
import com.example.android.notifications.AlarmScheduler;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.util.Key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private SupportMapFragment mMapFragment;
    private static final String TAG = "MapsActivity";
    private TextView textview_duration, textView_sourceAddr, textView_destAddr;
    private LatLng mLatlngSource, mLatlngDest;
    private String srcAddress, destAddress;
    private ImageButton button_refresh,button_reverse, button_directions;
    private int mDuration, mDurationTraffic;
    private double mDistance;
    private String mCongestion;
    private String mAvoidances = null;
    Marker m1, m2;
    private List<LatLng> mRouteMarkers = new ArrayList<>();
    Polyline mPolyLine;
    private double latSrc, latDest, longSrc, longDest;
    private boolean avoidHighways, avoidTolls;
    private static final boolean DBG = false;
    private Context mContext;
    private int m_ID;
    private int mDirection;
    private boolean mLogsAvailable = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        textview_duration = (TextView) findViewById(R.id.Text_mapduration);
        textView_sourceAddr = (TextView) findViewById(R.id.Text_mapsource);
        textView_destAddr = (TextView) findViewById(R.id.Text_mapdest);
        button_refresh = (ImageButton) findViewById(R.id.button_refresh);
        button_reverse = (ImageButton) findViewById(R.id.button_reverse);
        button_directions = (ImageButton) findViewById(R.id.button_directions);
        mContext = this;

        if(getSupportActionBar() == null)
            Log.e(TAG, "Action bar is null");
        else
            GenericFunctions.setUpActionBarColor(this, getResources().getColor(R.color.color_pink));

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        srcAddress = getIntent().getStringExtra("SOURCE");
        destAddress = getIntent().getStringExtra("DESTINATION");
        latSrc = getIntent().getDoubleExtra("LATITUDE_SOURCE", 0);
        latDest = getIntent().getDoubleExtra("LATITUDE_DESTINATION", 0);
        longSrc = getIntent().getDoubleExtra("LONGITUDE_SOURCE", 0);
        longDest = getIntent().getDoubleExtra("LONGITUDE_DESTINATION", 0);
        avoidHighways = getIntent().getBooleanExtra("AVOID_HIGHWAYS", false);
        avoidTolls = getIntent().getBooleanExtra("AVOID_TOLLS", false);
        m_ID = getIntent().getIntExtra("COL_ID", -1);
        mDirection = getIntent().getIntExtra("DIRECTION", AlarmScheduler.DIRECTION_SD);
        int logCount = DataLogger.getLogger().getLogCount(this, m_ID);
        Log.d(TAG, "Found:" + logCount + " rows for:" + m_ID);
        mLogsAvailable = (logCount > 0);
        invalidateOptionsMenu();

        //duplicate retrieval
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());//getPreferences(MODE_PRIVATE);
        avoidHighways = sharedPreferences.getBoolean("Highways", false);
        avoidTolls = sharedPreferences.getBoolean("Tolls", false);

        if(avoidHighways && avoidTolls){
            mAvoidances = "highways,tolls";
        }
        else if(avoidHighways){
            mAvoidances = "highways";
        }
        else if(avoidTolls){
            mAvoidances = "tolls";
        }
        else{
            mAvoidances = null;
        }

        mLatlngDest = new LatLng(latDest, longDest);
        mLatlngSource = new LatLng(latSrc, longSrc);

        if(srcAddress != null && destAddress!= null){
            String srcHtml = "<b>SOURCE: </b>"+ "<font color = \"blue\">" + srcAddress + "</font>";
            textView_sourceAddr.setText(Html.fromHtml(srcHtml));
            String destHtml = "<b>DESTINATION: </b>"+ "<font color = \"blue\">" + destAddress+ "</font>";
            textView_destAddr.setText(Html.fromHtml(destHtml));
        }

        button_directions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                StringBuilder builder = new StringBuilder("http://maps.google.com/maps?");
                builder.append("saddr=").append(latSrc).append(",").append(longSrc)
                       .append("&daddr=") .append(latDest).append(",").append(longDest);
                //builder.append("&mode=driving");
                if(avoidHighways && avoidTolls){
                    builder.append("&dirflg=th");
                }
                else if(avoidHighways){
                    builder.append("&dirflg=h");
                }
                else if(avoidTolls){
                    builder.append("&dirflg=t");
                }

                String uriStr = builder.toString();
                if(DBG) Log.d(TAG, "launch map with uri:" + uriStr);
                Intent intent= new Intent(Intent.ACTION_VIEW,
                        Uri.parse(uriStr));
                startActivity(intent);
            }
        });

        button_refresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //refresh the time duration by calling the async task
                textview_duration.setText(Html.fromHtml("<b>DURATION:</b>"));
                new LoadDirectionsTask().execute(m1.getPosition(), m2.getPosition());
            }
        });

        button_reverse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDirection = (mDirection == 0)?1:0;

                //reverse the texts
                String dummy = srcAddress;
                srcAddress = destAddress;
                destAddress = dummy;

                //also reverse the individual
                double tempdbl = latSrc;
                latSrc = latDest;
                latDest = tempdbl;

                tempdbl = longSrc;
                longSrc = longDest;
                longDest = tempdbl;



                String srcHtml = "<b>SOURCE: </b>"+ "<font color = \"blue\">" + srcAddress + "</font>";
                textView_sourceAddr.setText(Html.fromHtml(srcHtml));
                String destHtml = "<b>DESTINATION: </b>"+ "<font color = \"blue\">" + destAddress+ "</font>";
                textView_destAddr.setText(Html.fromHtml(destHtml));

                //interchange the markers
                LatLng temp = m1.getPosition();
                m1.setPosition(m2.getPosition());
                m1.setTitle(srcAddress);
                m2.setPosition(temp);
                m2.setTitle(destAddress);
                //finally fire the task to calculate time duration
                textview_duration.setText(Html.fromHtml("<b>DURATION:</b>"));
                new LoadDirectionsTask().execute(m1.getPosition(),m2.getPosition());
            }
        });

        mMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mMapFragment.getMapAsync(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_map_activity, menu);
        if (mLogsAvailable) {
            menu.findItem(R.id.action_charts).setVisible(true);
        }
        else {
            menu.findItem(R.id.action_charts).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_charts) {
            Intent intent = new Intent(MapsActivity.this, ChartViewer.class);
            intent.putExtra("SOURCE", srcAddress);
            intent.putExtra("DESTINATION", destAddress);
            intent.putExtra("COL_ID", m_ID);
            intent.putExtra("DIRECTION", mDirection);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        MarkerOptions option1 = new MarkerOptions().position(mLatlngSource).title(srcAddress).
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
        MarkerOptions option2 = new MarkerOptions().position(mLatlngDest).title(destAddress).
                icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
        m1 = googleMap.addMarker(option1);
        m2 = googleMap.addMarker(option2);

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(m1.getPosition());
        builder.include(m2.getPosition());
        final LatLngBounds bounds = builder.build();
        //CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,0);
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds,getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels, 0);
        googleMap.animateCamera(cu, new GoogleMap.CancelableCallback() {

            GoogleMap map = googleMap;

            @Override
            public void onFinish() {
                CameraUpdate cu2 = CameraUpdateFactory.zoomBy(-1.4f);
                map.moveCamera(cu2);
            }

            @Override
            public void onCancel() {
            }
        });

        googleMap.setTrafficEnabled(true);
        //find the duration
        new LoadDirectionsTask().execute(m1.getPosition(), m2.getPosition());

    }

    public static class BingMaps{
        @Key("resourceSets")
        public List<ResourcesSets> resourceSets;
    }

    public static class ResourcesSets{
        @Key("resources")
        public List<ResourcesBing> resources;
    }

    public static class ResourcesBing{
        @Key("trafficCongestion")
        public String congestion;
        @Key("travelDistance")
        public double travelDist;
        @Key("travelDuration")
        public Integer duration;
        @Key("travelDurationTraffic")
        public Integer durationTraffic;
        @Key("routeLegs")
        public List<RouteLegs> legs;
    }

    public static class RouteLegs{
        @Key ("itineraryItems")
        public List<ItineraryItems> itineraryItems;
    }

    public static class ItineraryItems{
        @Key("maneuverPoint")
        public ManeuverPoint maneuverPoint;
    }

    public static class ManeuverPoint{
        @Key("type")
        public String point;
        @Key("coordinates")
        public List<Double> coordinates;
    }

    public String getHtmlDuration(int integer, String color){
        int hours = integer/3600;
        integer = integer%3600; //left over seconds
        int minutes = integer/60;
        int seconds = integer%60;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder = stringBuilder.append("<font color = \"").
                append(color).
                append("\">");
        if(hours != 0) {
            stringBuilder = stringBuilder.append(hours).append(" hrs ");
        }
        if(minutes != 0){
            stringBuilder = stringBuilder.append(minutes).append(" min ");
        }
        if(minutes == 0 && hours == 0){
            stringBuilder = stringBuilder.append(seconds).append(" sec");
        }
        stringBuilder = stringBuilder.append("</font>");
        return stringBuilder.toString();
    }

    private class LoadDirectionsTask extends AsyncTask<LatLng, Integer, Integer>{

        @Override
        protected Integer doInBackground(LatLng... params) {
            LatLng src = params[0];
            LatLng dest = params[1];
            //call the function to calculated the duration
            return getDurationInTraffic(src,dest);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mRouteMarkers.clear();
            mRouteMarkers.add(m1.getPosition());
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer == 0){
                Log.e(TAG, "error calculating durations");
                Toast.makeText(mContext, "Network Error", Toast.LENGTH_SHORT).show();
                return;
            }
            mRouteMarkers.add(m2.getPosition());

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder = stringBuilder.append("<b>DURATION: </b>");
            String color = String.valueOf(getResources().getColor(R.color.teal));
            if(mCongestion.compareToIgnoreCase("Medium") == 0) {
                color = String.valueOf(getResources().getColor(R.color.darkorange));
            }
            else if(mCongestion.compareToIgnoreCase("Heavy")==0)
                color = String.valueOf(getResources().getColor(R.color.red));

            String timeString = getHtmlDuration(integer, color);
            if(timeString != null)
                stringBuilder = stringBuilder.append(timeString);

            //get the regular duration in same format (no color)
            String regTime = getHtmlDuration(mDuration, "black");
            stringBuilder = stringBuilder.append(" (").append(regTime).append(")");

            stringBuilder.append("<br/><b>DISTANCE: </b>");
            stringBuilder.append((int)Math.round(mDistance)).append(" miles");


            textview_duration.setText(Html.fromHtml(stringBuilder.toString()));

            //also draw the polylines
            if(mPolyLine != null){
                mPolyLine.remove();
            }
            PolylineOptions polylineOptions= new PolylineOptions();
            for(LatLng latLng:mRouteMarkers){
                polylineOptions.add(latLng);
                if(DBG) Log.d(TAG, "lat:" + latLng.latitude + "long:" + latLng.longitude);
            }
            polylineOptions.color(Color.BLUE);
            mPolyLine = mMap.addPolyline(polylineOptions);

        }

        private int getDurationInTraffic(LatLng src, LatLng dest){

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            final JsonFactory json = new AndroidJsonFactory();
            HttpRequestFactory factory = transport.createRequestFactory(new HttpRequestInitializer() {
                @Override
                public void initialize(HttpRequest httpRequest) throws IOException {
                    httpRequest.setParser(new JsonObjectParser(json));
                }
            });
            GenericUrl url = new GenericUrl("http://dev.virtualearth.net/REST/V1/Routes/Driving");
            url.put("wp.0",new StringBuilder().append(src.latitude).append(',').append(src.longitude).toString());
            url.put("wp.1", new StringBuilder().append(dest.latitude).append(',').append(dest.longitude).toString());
            url.put("du", "mi");
            if(mAvoidances != null) {
                url.put("avoid", mAvoidances);
            }
            url.put("key", "YOUR_BING_KEY_HERE");

            try {
                HttpRequest request = factory.buildGetRequest(url);
                HttpResponse response = request.execute();
                BingMaps bingMaps = response.parseAs(BingMaps.class);
                List<ResourcesSets> resourcesSet = bingMaps.resourceSets;
                List<ResourcesBing> resourcesBing =  resourcesSet.get(0).resources;
                ResourcesBing resultRoute = resourcesBing.get(0);
                if(DBG) Log.d(TAG, "congestion=" + resultRoute.congestion + " distance:"
                        + resultRoute.travelDist + " duration:" + resultRoute.duration + "traffic duration:" + resultRoute.durationTraffic);
                //save the found values
                mCongestion = resultRoute.congestion;
                mDistance = resultRoute.travelDist;
                mDuration = resultRoute.duration;
                mDurationTraffic=resultRoute.durationTraffic;

                //Also calculate all the manuever points for creating route lines
                List<RouteLegs> legs = resultRoute.legs;
                if(legs != null && legs.size() != 0){
                    //add source to the markerlist
                    mRouteMarkers.clear();
                    List<ItineraryItems> items = legs.get(0).itineraryItems;
                    if(items != null){
                        for(ItineraryItems item:items) {
                            double latit = item.maneuverPoint.coordinates.get(0);
                            double longit = item.maneuverPoint.coordinates.get(1);
                            mRouteMarkers.add(new LatLng(latit, longit));
                            if(DBG) Log.d(TAG, "manuever point: " + latit + ", " + longit);
                        }
                    }
                }
                return resultRoute.durationTraffic;

            } catch (IOException e){
                Log.e(TAG, "Error creating http request");
                //Toast.makeText(mContext, "Network Error", Toast.LENGTH_SHORT).show();
            }
            return 0;
        }
    }
}
