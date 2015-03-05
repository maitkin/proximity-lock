package com.m3sys.proxlock.service;

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

import android.app.Service;
import android.app.admin.DevicePolicyManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.ComponentName;
import android.content.Intent;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import com.m3sys.proxlock.MainActivity;
import com.m3sys.proxlock.db.DataBridge;
import com.m3sys.proxlock.db.PLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.util.List;

public class ProxSecService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    static final String TAG = MainActivity.BASE_TAG + "-ProxSecService";
    DevicePolicyManager mDevicePolicyManager;
    ComponentName mAdminComponentName;
    GoogleApiClient mGoogleApiClient;
    PowerManager.WakeLock wakeLock;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothManager mBluetoothManager;

    boolean mBluetoothSupport = false;
    boolean misLocked = false;

    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {


        /*

        See http://stackoverflow.com/questions/23947874/how-to-solve-the-bluetoothgatt-android-os-deadobjectexception-error-happened-i
        for the deadobjectexception code...

        */
        try {
            // Setup bluetooth
            mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);

            // Bluetooth available?
            if (mBluetoothManager.getAdapter() != null) {

                List<BluetoothDevice> connectedDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);        // If there are paired devices
                if (connectedDevices.size() > 0) {
                    // Loop through paired devices
                    for (BluetoothDevice device : connectedDevices) {
                        // Add the name and address to an array adapter to show in a ListView
                        Log.d(TAG, "found connected bluetooth device " + device.getName() + "" + device.getAddress());
                    }
                } else {
                    Log.d(TAG, "No connected bluetooth devices");
                }
            }
            else {
                Log.w(TAG,"Bluetooth not present (or enabled)");
            }
        }

        catch(Exception e) {
            // Can't actually catch the DeadObjectException itself for some reason...*shrug*.
            if( e instanceof android.os.DeadObjectException ) {
                Log.e(TAG,"bluetooth deadobjectexception caught");
                // notify user through a dialog or something that they should either restart bluetooth or their phone.
                // another option is to reset the stack programmatically.
            }
            else {
                Log.e(TAG,"error caught "+e.getMessage());
                e.printStackTrace();
            }
        }


        // Do we have admin?
        Log.d(TAG, "service onStartCommand");
        mAdminComponentName =  new ComponentName(this, MyAdminReceiver.class);
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isAdmin = mDevicePolicyManager.isAdminActive(mAdminComponentName);
        Log.d(TAG,"isAdminActive="+isAdmin);

        if (!isAdmin) {
            enableAdmin();
        }


        // Start google client for current location
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "");
        wakeLock.acquire();

        // when connected, will check location then release wakelock
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();

        return Service.START_NOT_STICKY;
    }


    // Starts an activity where we can enable admin
    private void enableAdmin() {
        Log.d(TAG,"enableAdmin");
        Intent dialogIntent = new Intent(getBaseContext(), EnableAdminActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getApplication().startActivity(dialogIntent);
    }

    // If the phone has already had it's password set, don't lock the phone, otherwise do lock it
    private void lockPhone() {

        if (misLocked) {
            Log.d(TAG,"phone is already locked");
            return;
        }
        boolean isAdmin = mDevicePolicyManager.isAdminActive(mAdminComponentName);

        if (isAdmin) {
            mDevicePolicyManager.setPasswordQuality(mAdminComponentName,DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
            mDevicePolicyManager.setPasswordMinimumLength(mAdminComponentName, 4);

            if (!mDevicePolicyManager.isActivePasswordSufficient()) {
                Log.d(TAG, "Establishing password");
                boolean result = mDevicePolicyManager.resetPassword("3364", DevicePolicyManager.RESET_PASSWORD_REQUIRE_ENTRY);
                mDevicePolicyManager.lockNow();
                Log.d(TAG, "locked phone");
                misLocked = true;
            }
            else {
                Log.d(TAG,"phone has already been secured, not locking");
            }

        }else{
            Log.e(TAG, "unable to lock phone, admin not enabled!");
        }

    }


    /*
        There apparently isn't a direct way to tell if a password is set for a phone, so we
        reset the password every time here.
     */
    private void unlockPhone() {

        boolean isAdmin = mDevicePolicyManager.isAdminActive(mAdminComponentName);

        if (isAdmin) {

            mDevicePolicyManager.setPasswordQuality(mAdminComponentName, DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED);
            mDevicePolicyManager.setPasswordMinimumLength(mAdminComponentName, 0);
            Log.d(TAG, "Resetting password");
            boolean result = mDevicePolicyManager.resetPassword("", 0);
            misLocked = false;
        }
        else {
            Log.e(TAG, "not admin, can't unsecure phone");
        }
    }


    // Required implementation for Service
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }


    /**
     * Runs when a GoogleApiClient object successfully connects.
     */

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG,"Connected to Google");

        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {

            DataBridge db = new DataBridge(this);
            db.open();
            List<PLocation> safeLocs = db.getAllLocations();
            db.close();

            if (safeLocs.isEmpty()) {
                Log.d(TAG,"No stored safe locations");
                return;
            }

            Log.d(TAG,"last location lat="+mLastLocation.getLatitude()+" long="+mLastLocation.getLongitude());

            float distance[] = new float[3];
            boolean securePhone = true;

            for (PLocation location : safeLocs) {
                Location.distanceBetween(location.getLatitude(), location.getLongitude(), mLastLocation.getLatitude(), mLastLocation.getLongitude(), distance);
                Log.d(TAG, "computed distance to " + location.getName() + " of " + String.valueOf(distance[0]) + " meters");

                // First valid location, break and unlock
                if (distance[0] < 100) {
                    securePhone = false;
                    break;
                }
            }

            if (securePhone) lockPhone(); else unlockPhone();

        } else {
            Log.e(TAG, "no location detected!");
        }

        wakeLock.release();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode() + ":" + result.toString());
        if (wakeLock.isHeld()) wakeLock.release();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "Connection suspended");
        if (wakeLock.isHeld()) wakeLock.release();
    }
}
