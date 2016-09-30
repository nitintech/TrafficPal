package com.example.android.notifications;

import android.util.Log;

import com.example.android.trafficpal.MainActivity;
import com.google.android.gms.maps.model.LatLng;
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
import java.util.List;

/**
 * Created by niarora on 11/16/2015.
 */
public class TrafficDurationCalc {
    private static String TAG = "TrafficDurationCalc";

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


    public static ResourcesBing getDurationInTraffic(LatLng src, LatLng dest, boolean avoidHighways, boolean avoidTolls){

        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        String mAvoidances = null;
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
        if(avoidHighways && avoidTolls){
            mAvoidances = "highways,tolls";
        }
        else if(avoidHighways){
            mAvoidances = "highways";
        }
        else if(avoidTolls){
            mAvoidances = "tolls";
        }

        if(mAvoidances != null){
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
            Log.d(TAG, "congestion=" + resultRoute.congestion + " distance:"
                    + resultRoute.travelDist + " duration:" + resultRoute.duration + "traffic duration:" + resultRoute.durationTraffic);
            //save the found values
            String congestion = resultRoute.congestion;
            double distance = resultRoute.travelDist;
            int duration = resultRoute.duration;
            int durationTraffic=resultRoute.durationTraffic;

            return resultRoute;
        }
        catch (IOException e){
            Log.e(TAG, "Error creating http request");
        }
        return null;
    }

    public static String getDuration(int integer){
        int hours = integer/3600;
        integer = integer%3600; //left over seconds
        int minutes = integer/60;
        int seconds = integer%60;
        StringBuilder stringBuilder = new StringBuilder();
        if(hours != 0) {
            stringBuilder = stringBuilder.append(hours).append(" hrs ");
        }
        if(minutes != 0){
            stringBuilder = stringBuilder.append(minutes).append(" min ");
        }
        if(minutes == 0 && hours == 0){
            stringBuilder = stringBuilder.append(seconds).append(" sec");
        }
        return stringBuilder.toString();
    }
}
