package com.example.dudegenie.myapplication;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.demach.konotor.Konotor;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Harsh on 12-05-2015.
 */
public class LandingPage extends Activity {

    private SensorManager mSensorManager;
    private Accelerometer mSensorListener;
    private static String TAG="Error";
    private Boolean flag = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.landing_page);
        Button myFeedbackButton = (Button) findViewById(R.id.myFeedbackButton);
        Bundle extras = getIntent().getExtras();
        String name = extras.getString("FullName");
        String email = extras.getString("Email");
        TextView view = (TextView) findViewById(R.id.text_name);
        view.setText("Welcome" + " " + name + "(" + email + ")");
        Konotor.getInstance(getApplicationContext())
                .withUserName(name)            // optional name by which to display the user
                .withIdentifier("My_User_100")            // optional unique identifier for your reference
                .withUserMeta("age", "19")            // optional metadata for your user
                .withUserEmail(email)        // optional email address of the user
                .init("f7fdbbb8-79bd-4d8e-8da2-8f6ee9ac37b3", "6c2e8fe5-a22c-4769-9180-b57b9ea07fd3");

        flag = displayGpsStatus();
        if (flag) {
            Location location = getBestLocation();
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(lat, lon, 1);
                if (addresses.size() > 0) {
                    cityName = addresses.get(0).getLocality();
                    Toast.makeText(getApplicationContext(), "You're in "+cityName, Toast.LENGTH_LONG).show();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else{
            alertbox("Gps Status!!", "Your GPS is: OFF");
        }

            myFeedbackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Konotor.getInstance(getApplicationContext())
                .launchFeedbackScreen(LandingPage.this);
            }
        });
        shake();
    }

    /*----Method to Check GPS is enable or disable ----- */
    private Boolean displayGpsStatus() {
        ContentResolver contentResolver = getBaseContext()
                .getContentResolver();
        boolean gpsStatus = Settings.Secure
                .isLocationProviderEnabled(contentResolver,
                        LocationManager.GPS_PROVIDER);
        if (gpsStatus) {
            return true;

        } else {
            return false;
        }
    }

    /*----------Method to create an AlertBox ------------- */
    protected void alertbox(String title, String mymessage) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Please enable location services")
                .setCancelable(false)
                .setTitle("Location Services Disabled")
                .setPositiveButton("Enable",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish the current activity
                                // AlertBoxAdvance.this.finish();
                                Intent myIntent = new Intent(
                                        Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                startActivity(myIntent);
                                dialog.cancel();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // cancel the dialog box
                                dialog.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void shake() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorListener = new Accelerometer();

        mSensorListener.setOnShakeListener(new Accelerometer.OnShakeListener() {

            public void onShake() {
                Toast.makeText(getApplicationContext(), "Shake Detected!", Toast.LENGTH_SHORT).show();
                Konotor.getInstance(getApplicationContext())
                        .launchFeedbackScreen(LandingPage.this);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(mSensorListener,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(mSensorListener);
        super.onStop();
    }

    private Location getBestLocation() {
        Location gpslocation = getLocationByProvider(LocationManager.GPS_PROVIDER);
        Location networkLocation =
                getLocationByProvider(LocationManager.NETWORK_PROVIDER);
        // if we have only one location available, the choice is easy
        if (gpslocation == null) {
            Log.d(TAG, "No GPS Location available.");
            return networkLocation;
        }
        if (networkLocation == null) {
            Log.d(TAG, "No Network Location available");
            return gpslocation;
        }
        // a locationupdate is considered 'old' if its older than the configured
        // update interval. this means, we didn't get a
        // update from this provider since the last check
        long old = System.currentTimeMillis() - 2000;
        boolean gpsIsOld = (gpslocation.getTime() < old);
        boolean networkIsOld = (networkLocation.getTime() < old);
        // gps is current and available, gps is better than network
        if (!gpsIsOld) {
            Log.d(TAG, "Returning current GPS Location");
            return gpslocation;
        }
        // gps is old, we can't trust it. use network location
        if (!networkIsOld) {
            Log.d(TAG, "GPS is old, Network is current, returning network");
            return networkLocation;
        }
        // both are old return the newer of those two
        if (gpslocation.getTime() > networkLocation.getTime()) {
            Log.d(TAG, "Both are old, returning gps(newer)");
            return gpslocation;
        } else {
            Log.d(TAG, "Both are old, returning network(newer)");
            return networkLocation;
        }
    }

    /**
     * get the last known location from a specific provider (network/gps)
     */
    private Location getLocationByProvider(String provider) {
        Location location = null;

        LocationManager locationManager = (LocationManager) getApplicationContext()
                .getSystemService(Context.LOCATION_SERVICE);
        try {
            if (locationManager.isProviderEnabled(provider)) {
                location = locationManager.getLastKnownLocation(provider);
            }
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "Cannot access Provider " + provider);
        }
        return location;
    }
}