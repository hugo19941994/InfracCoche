/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 *
 * Modulo encargado de actualizar el mapa
 */

package com.example.uemcar;

import android.app.Activity;
import android.location.Location;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class Map {

    Activity activity;
    private GoogleMap mMap;

    public Map(final Activity act) {
        activity = act;
    }

    public void onCreate(){
        setUpMapIfNeeded();
    }

    public void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((MapFragment) activity.getFragmentManager()
                    .findFragmentById(R.id.map)).getMap();
            mMap.setMyLocationEnabled(true);
        }
    }

    public void putMarker(Location location){
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        mMap.addMarker(new MarkerOptions().position(new LatLng(lat, lon)).title("Marker"));
    }

    /**
     * Pone un "pin" en la ultima posición GPS
     */
    public void putMarkerHere(){
        putMarker(((MainActivity) activity).gps.mLastLocation);
    }

    public void moveCamera(CameraUpdate cu){
        mMap.animateCamera(cu);
    }

}
