package se.magnuspaulsson.tidtilltuben;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import se.magnuspaulsson.tidtilltuben.domain.Departure;
import se.magnuspaulsson.tidtilltuben.helpers.ParseHelper;

public class MainActivity extends AppCompatActivity {
    private Context context = this;

    private LinearLayout mLinearLayout;
    private Button mUpdateBtn;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mUserLocation;

    private HashMap<String, List<Departure>> mResultMap;
    private HashMap<String, List<Departure>> mResultMapCopy;

    private boolean requestChainFinished = false;
    private boolean requestLoopedFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUpdateBtn = (Button) findViewById(R.id.updateButton);
        mLinearLayout = (LinearLayout) findViewById(R.id.linearLayout);

        // TODO Remove?
        mUpdateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mResultMap.clear();
                mLinearLayout.removeAllViews();
            }
        });

        // Start fetching the information (device location -> stations -> departure times etc.)
        requestUserLocation();

        // schedule update repeated requests and display them
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d("Display?", "" + requestChainFinished + " och " + requestLoopedFinished);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // stuff that updates ui
                        if(requestChainFinished){
                            displayInformation();
                            requestChainFinished = false;
                        }
                    }
                });
            }
        }, 3000, 5000); // 5 seconds

        new Timer().scheduleAtFixedRate(new TimerTask() {
            int i = 0;

            @Override
            public void run() {
                Log.d("Requesting departures", "" + ++i);

                // keep checking departure times
                requestUserLocation();
            }
        }, 5000, 60000);// 60 seconds

    }

    private void displayInformation() {
        Log.d(this.getClass().getSimpleName(),"Inside displayInformation");

        mResultMapCopy = mResultMap;

        if(mResultMapCopy != null && !mResultMapCopy.isEmpty()){
            mLinearLayout.removeAllViews();
            Iterator it = mResultMapCopy.keySet().iterator();
            while(it.hasNext()){
                String key = (String) it.next();
                TextView colorOfLine = new TextView(this);
                colorOfLine.setText(key);

                if(key.toUpperCase().contains("GRÖNA")){
                    colorOfLine.setTextColor(Color.GREEN);
                }

                if(key.toUpperCase().contains("RÖDA")){
                    colorOfLine.setTextColor(Color.RED);
                }

                if(key.toUpperCase().contains("BLÅA")){
                    colorOfLine.setTextColor(Color.BLUE);
                }

                mLinearLayout.addView(colorOfLine);

                ArrayList<Departure> departures = (ArrayList<Departure>) mResultMapCopy.get(key);

                Collections.sort(departures);

                for(Departure d:departures){
                    TextView departure = new TextView(this);
                    departure.setText(d.getStationName() + " " + d.getDepartureString());
                    mLinearLayout.addView(departure);
                }
            }
        }
    }

    private void requestDepartureTime(ArrayList<Integer> siteIds) {
        Log.d(this.getClass().getSimpleName(), "Inside requestDepartureTime");

        for(int i = 0; i < siteIds.size(); i++) {

            int siteId = siteIds.get(i);

            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://api.sl.se/api2/realtimedeparturesV4.json?key=fd96b62f0e3f4dcf9b629270ed1eda8d&siteid=" + siteId + "&timewindow=15&Bus=false&Train=false&Tram=false&Ship=false";

            JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

                @Override
                public void onResponse(JSONObject response) {
                    if(response == null){
                        return;
                    }
                    try {

                        int statusCode = response.getInt("StatusCode");
                        String message = response.getString("Message");

                        JSONObject responseData = response.getJSONObject("ResponseData");
                        JSONArray metros = responseData.getJSONArray("Metros");

                        // new result
                        mResultMap = new HashMap<String, List<Departure>>();

                        requestLoopedFinished = false;

                        for (int i = 0; i < metros.length(); i++) {

                            JSONObject metro = metros.getJSONObject(i);
                            String groupOfLine = metro.getString("GroupOfLine").toUpperCase();
                            String destination = metro.getString("Destination");
                            String displayTime = metro.getString("DisplayTime");
                            String expectedDateTime = metro.getString("ExpectedDateTime"); // 2018-01-05T09:44:30

                            Date date = ParseHelper.parseDateTimeString(expectedDateTime);

                            Departure departure = new Departure();
                            departure.setStationName(destination);
                            departure.setDepartureString(displayTime);
                            departure.setDeparture(date);

                            if (mResultMap.get(groupOfLine) == null) {
                                ArrayList<Departure> departures = new ArrayList<Departure>();
                                departures.add(departure);
                                mResultMap.put(groupOfLine, departures);
                            } else {
                                mResultMap.get(groupOfLine).add(departure);
                            }

                            // Are we done?
                            if(i == (metro.length() - 1)){
                                Log.d("DONE?", "YES!");
                                requestLoopedFinished = true;
                            } else {
                                Log.d("DONE?", "NO! (" + i + " of " + (metro.length() -1) + ")");
                            }
                        }

                    } catch (JSONException e) {
                        Log.e("Request error","Incomplete JSON response \n" + e.getMessage());
                        //e.printStackTrace();
                    }

                }
            }, new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // TODO Auto-generated method stub
                    Log.e("Volley error", error.getMessage());
                }
            });

            // Access the RequestQueue through your singleton class.
            MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);

            Log.d("i", "i = " + i + " size = " + (siteIds.size() - 1));
            if(i == (siteIds.size() - 1)){
                requestChainFinished = true;
            }
        }
    }

    private void requestNearByStations(Location location) {
        Log.d(this.getClass().getSimpleName(), "Inside requestNearByStations");

        requestChainFinished = false;

        // Get lat and long from location
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://api.sl.se/api2/nearbystops.json?key=903b7220cae542498d649d3e6ce20053&originCoordLat="+latitude+"&originCoordLong="+longitude+"&maxresults=10&radius=750";

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                if(response == null){
                    return;
                }
                try {

                    if (response == null) {
                        return;
                    }

                    if (response.getJSONObject("LocationList") != null) {
                        JSONObject locationList = response.getJSONObject("LocationList");

                        Object stopLocationObject = locationList.get("StopLocation");

                        ArrayList<Integer> siteIds = new ArrayList<Integer>();


                        if (stopLocationObject instanceof JSONObject) {

                            JSONObject stopLocation = (JSONObject) stopLocationObject;

                            String stationName = stopLocation.getString("name");
                            int id = stopLocation.getInt("id");
                            int dist = stopLocation.getInt("dist");

                            int siteId = id % 10000;

                            siteIds.add(siteId);

                            requestDepartureTime(siteIds);

                        } else {

                        JSONArray stopLocations = (JSONArray) stopLocationObject;

                        for (int i = 0; i < stopLocations.length(); i++) {

                            JSONObject stopLocation = stopLocations.getJSONObject(i);

                            String stationName = stopLocation.getString("name");
                            int id = stopLocation.getInt("id");
                            int dist = stopLocation.getInt("dist");

                            int siteId = id % 10000;

                            siteIds.add(siteId);
                        }

                        if (!siteIds.isEmpty()) {

                            requestDepartureTime(siteIds);

                        }
                    }
                }

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                // TODO Auto-generated method stub

            }
        });

        // Access the RequestQueue through your singleton class.
        MySingleton.getInstance(this).addToRequestQueue(jsObjRequest);
    }

    private void requestUserLocation() {
        Log.d(this.getClass().getSimpleName(), "Inside requestUserLocation");

        //Mock location

        Location location = new Location("temp");
        location.setLatitude(59.293525);
        location.setLongitude(18.083519);

        requestNearByStations(location);

        /*

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mFusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {

                            requestNearByStations(location);

                        }
                    }
                });
                */
    }

}
