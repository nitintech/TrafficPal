package com.example.android.trafficpal;

import android.content.Context;
import android.text.style.CharacterStyle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataBufferUtils;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLngBounds;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by niarora on 10/14/2015.
 */
public class PlacesAutoCompleteAdapter  extends ArrayAdapter<String> implements Filterable{

    private static final String TAG = "PlacesAutoCompleteAdapter";
    private ArrayList<AutocompletePrediction> mResultList;
    private LatLngBounds mBounds;
    private GoogleApiClient mGoogleApiClient;
    private List<String> mStringResultList;
    private AutocompleteFilter mFilter;
    public Map<String, String> addressMap;


    public PlacesAutoCompleteAdapter(Context context, GoogleApiClient googleApiClient, LatLngBounds latLngBounds, AutocompleteFilter filter){
        //super(context, android.R.layout.simple_expandable_list_item_2, android.R.id.text1);
        super(context, android.R.layout.simple_expandable_list_item_2, android.R.id.text1);
        mGoogleApiClient = googleApiClient;
        mBounds=latLngBounds;
        mFilter = filter;
        mStringResultList = new ArrayList<String>();
        addressMap = new HashMap<>();
    }

    @Override
    public Filter getFilter() {
        final Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults filterResults= new FilterResults();
                //call a function to get the hints based in charsequence constraint
                List<String> results = getAutocomplete(constraint);

                if(results != null) {
                    filterResults.values = results;
                    filterResults.count = results.size();
                }
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                if(results != null && results.count != 0)
                    notifyDataSetChanged();
                else
                    notifyDataSetInvalidated();
            }
        };
        return filter;
    }

    @Override
    public int getCount() {
        return mStringResultList.size();
    }

    @Override
    public String getItem(int position) {
        if(position < mStringResultList.size())
            return mStringResultList.get(position);
        else
            return "";
    }
    private List<String> getAutocomplete(CharSequence constraint) {
        if (mGoogleApiClient.isConnected()) {
            //Log.i(TAG, "Starting autocomplete query for: " + constraint);

            // Submit the query to the autocomplete API and retrieve a PendingResult that will
            // contain the results when the query completes.
            PendingResult<AutocompletePredictionBuffer> results =
                    Places.GeoDataApi
                            .getAutocompletePredictions(mGoogleApiClient, constraint.toString(),
                                    mBounds, mFilter);

            // This method should have been called off the main UI thread. Block and wait for at most 60s
            // for a result from the API.
            AutocompletePredictionBuffer autocompletePredictions = results
                    .await(60, TimeUnit.SECONDS);

            // Confirm that the query completed successfully, otherwise return null
            final Status status = autocompletePredictions.getStatus();
            if (!status.isSuccess()) {
                /*Toast.makeText(getContext(), "Error contacting API: " + status.toString(),
                        Toast.LENGTH_SHORT).show();*/
                Log.e(TAG, "Error getting autocomplete prediction API call: " + status.toString());
                autocompletePredictions.release();
                return null;
            }

            Log.i(TAG, "Query completed. Received " + autocompletePredictions.getCount()
                    + " predictions.");

            Iterator<AutocompletePrediction> iterator = autocompletePredictions.iterator();
            mStringResultList.clear();
            addressMap.clear();
            while(iterator.hasNext()){
                AutocompletePrediction prediction = iterator.next();
                String placeID = prediction.getPlaceId().toString();
                String address = prediction.getFullText(null).toString();
                if(placeID != null)
                    addressMap.put(address, placeID);
                /*if(placeID != null){
                    PendingResult<PlaceBuffer> placesBuffer = Places.GeoDataApi.getPlaceById(mGoogleApiClient, placeID);
                    PlaceBuffer places = placesBuffer.await(60, TimeUnit.SECONDS);
                    if(places.getStatus().isSuccess()){

                    }else{
                        places.release();
                    }
                    address = address + "$" + placeID;
                }*/
                mStringResultList.add(address);
            }
            autocompletePredictions.release();
            return mStringResultList;

        }
        Log.e(TAG, "Google API client is not connected for autocomplete query.");
        return null;
    }

}
