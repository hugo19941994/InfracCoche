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
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class GPS implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    public GPS(final  Activity act) {
         activity = act;
    }

    Activity activity;
    private GoogleApiClient mGoogleApiClient;
    public Location mLastLocation;
    private LocationRequest mLocationRequest;
    private boolean started = true;
    private Handler handler;

    private TextView t1, t2;

    // Gyro
    float azimut;
    float[] orientation = new float[3];
    float[] rMat = new float[9];

    public final void onCreate() {
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        createLocationRequest();
        handler = new Handler();

        t1 = (TextView) activity.findViewById(R.id.textView);
        t2 = (TextView) activity.findViewById(R.id.textView2);

        // Gyro
        SensorManager mSensorManager;
        Sensor mSensor;
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

            t1.setText("Speed: " + Float.toString(speed) + "\nAzimut: " + Float.toString(azimut));
            t2.setText("Last traffic signals:");
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

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(mLastLocation.getLatitude(),
                        mLastLocation.getLongitude())).zoom(16.0f).build();
        CameraUpdate cameraUpdate = CameraUpdateFactory
                .newCameraPosition(cameraPosition);

        ((MainActivity) activity).map.moveCamera(cameraUpdate);
        start();
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
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation == null) {
            Toast.makeText(activity, "No location detected", Toast.LENGTH_LONG).show();
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
    }

    public void removeCallback() {
        handler.removeCallbacks(runnable);
    }

}
