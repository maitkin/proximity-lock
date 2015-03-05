package com.m3sys.proxlock;

/**
 Proximity-Lock
 Copyright (C) 2014  Marc W. Aitkin

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

 */

import android.app.AlertDialog;

import android.content.DialogInterface;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import android.location.Location;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.marc.proximitysecurity.R;
import com.m3sys.proxlock.db.DataBridge;
import com.m3sys.proxlock.db.PLocation;
import com.m3sys.proxlock.service.AlarmReceiver;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;

import java.util.List;

public class MainActivity extends ActionBarActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener, AddSafeLocationDialogFragment.NoticeDialogListener {

    public static final String BASE_TAG = "m3.proxlock";
    private static final String TAG = BASE_TAG + "-MainActivity";
    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected LocationRequest mLocationRequest;
    private TextView mLatitudeText;
    private TextView mLongitudeText;
    private DataBridge dB;
    private int listViewPosition = 0;


    private class LocationListItem {

        public LocationListItem(PLocation l, Location c) {
            mSafe = l;
            mCurrent = c;
        }
        public PLocation mSafe;
        public Location mCurrent;

        public String toString() {
            float distance[] = new float[3];
            Location.distanceBetween(mSafe.getLatitude(), mSafe.getLongitude(), mCurrent.getLatitude(), mCurrent.getLongitude(), distance);
            return mSafe.getName() + "   --->   " + distance[0] + " meters";
        }
    }

    private ArrayAdapter<LocationListItem> mSafeLocations;



    /*
        MAIN View Interactions
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.d(TAG,"onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSafeLocations = new ArrayAdapter<LocationListItem>(this,android.R.layout.simple_list_item_1,android.R.id.text1);

        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(mSafeLocations);

        
        buildGoogleApiClient();

        // Set initial alarm, reboot will set the rest
        AlarmReceiver alarm = new AlarmReceiver();
        alarm.SetAlarm(this);


        // Delete entry dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {

                LocationListItem item = mSafeLocations.getItem(listViewPosition);
                dB.deleteLocation(item.mSafe.getId());
                updateGui(mLastLocation);
                Toast.makeText(MainActivity.this, "Location deleted", Toast.LENGTH_SHORT).show();

                // User clicked OK button
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
            }
        });

        builder.setMessage("Delete this entry?");
        final AlertDialog dialog = builder.create();


// Create the AlertDialog


        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {

                dialog.show();
                listViewPosition = position;

                LocationListItem item = mSafeLocations.getItem(listViewPosition);
                Log.d(TAG, "deleteing " + item.mSafe.getName());
                return true;
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
    protected void onStart() {
        super.onStart();
        dB = new DataBridge(this);
        dB.open();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        dB.close();
        super.onStop();
    }


    protected void onDestroy() {
        dB.close();
        super.onDestroy();
    }

    /*
        Responding to user interaction
     */

    public void setHomeClicked(View w) {
        Button b = (Button) w;

        DialogFragment newFragment = new AddSafeLocationDialogFragment();
        newFragment.show(getSupportFragmentManager(), "Add Location");


    }


    public void onDialogPositiveClick(DialogFragment dialog) {
        // User touched the dialog's positive button
        EditText t = (EditText) dialog.getDialog().findViewById(R.id.name);

        if (mLastLocation == null) return;

        if (t.getText().toString().isEmpty()) {
            Toast.makeText(getApplicationContext(),"No name entered!",Toast.LENGTH_SHORT).show();
            return;
        }

        dB.insertLocation(t.getText().toString(), mLastLocation.getLatitude(), mLastLocation.getLongitude(), true, PLocation.LATLONG);

        updateGui(mLastLocation);
        Toast.makeText(getApplicationContext(), "Home Saved!", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "saving " + t.getText().toString());
    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        // User touched the dialog's negative button

    }


    protected void updateGui(Location l) {
        mLatitudeText.setText(String.valueOf(l.getLatitude()));
        mLongitudeText.setText(String.valueOf(l.getLongitude()));
        updateLocations(l);
    }


    public void updateLocations(Location current) {
        mSafeLocations.clear();
        List<PLocation> locations = dB.getAllLocations();

        for (PLocation l : locations) {
            mSafeLocations.add(new LocationListItem(l,current));
        }

    }


    /*
        GOOGLE API Interactions
     */


    protected synchronized void buildGoogleApiClient() {
        Log.d(TAG, "buildingGoogleClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG,"Connected to Google");

        createLocationRequest();

        startLocationUpdates();

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        mLatitudeText = (TextView) findViewById((R.id.latText));
        mLongitudeText = (TextView) findViewById((R.id.longText));

        //mHomeList = (ListView) findViewById(R.id.listView);

        if (mLastLocation != null) {
            Log.d(TAG,"last location lat="+mLastLocation.getLatitude()+" long="+mLastLocation.getLongitude());
            updateGui(mLastLocation);
        } else {
            Log.e(TAG,"no location detected!");
        }
    }

    /**
     * Runs when a GoogleApiClient object unsuccessfully connects.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        Log.i(TAG,result.toString());
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }


    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG,"onLocationChanged"+location.toString());
        mLastLocation = location;
        updateGui(location);
    }


}
