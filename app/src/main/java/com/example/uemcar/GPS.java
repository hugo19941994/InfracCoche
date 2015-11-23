/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.util.FloatMath;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class GPS implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public GPS(final  Activity act){
         activity = act;
    }

    Activity activity;
    private GoogleApiClient mGoogleApiClient;
    public Location mLastLocation;
    private LocationRequest mLocationRequest;
    private boolean started = true;

    private Handler handler;

    private Button button;

    // Gyro
    private SensorManager mSensorManager;
    private Sensor mSensor;
    float azimut;
    float[] orientation = new float[3];
    float[] rMat = new float[9];

    public final void onCreate(){
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        createLocationRequest();
        handler = new Handler();

        //start();

        button = (Button) activity.findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                ((MainActivity) activity).map.putMarker(mLastLocation); //Tightly coupled
                button.setText(Float.toString((mLastLocation.getSpeed() * 3600) / 1000));
            }
        });

        // Gyro
        mSensorManager = (SensorManager) activity.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mSensorManager.registerListener(mListener, mSensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Float speed = ((mLastLocation.getSpeed()*3600)/1000);
            if (speed > 120)
                ((MainActivity) activity).map.putMarker(mLastLocation); //Tightly coupled
            button.setText("Speed: " + Float.toString(speed) + "\nAzimut: " + Float.toString(azimut));
            if(started) {
                start();
            }
        }
    };

    public void start() {
        started = true;
        handler.postDelayed(runnable, 100);
    }

    private final SensorEventListener mListener = new SensorEventListener() {
        // Gyro
        public void onSensorChanged(SensorEvent event) {
            SensorManager.getRotationMatrixFromVector( rMat, event.values );
            // get the azimuth value (orientation[0]) in degree
            azimut = (float) ( Math.toDegrees( SensorManager.getOrientation( rMat, orientation )[0] ) + 360 ) % 360;
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        start();
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(activity)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation == null) {
            Toast.makeText(activity, "No location detected", Toast.LENGTH_LONG).show();
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
    }

    public void removeCallback() {
        handler.removeCallbacks(runnable);
    }

}
