/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class MainActivity extends Activity {

    public GPS gps = new GPS(this);
    public Map map = new Map(this);
    public Camera camera = new Camera(this);

    static {
    	System.loadLibrary("opencv_java3");
        System.loadLibrary("jniPart");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawer);

        // Drawer
        String[] choices;
        ListView mDrawerList;

        choices = new String[6];
        choices[0] = "Results";
        choices[1] = "RGB";
        choices[2] = "Grey";
        choices[3] = "HLS";
        choices[4] = "Red Hue";
        choices[5] = "Contours"; // Añadir contours filtered y contours with circles
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        LayoutInflater inflater = getLayoutInflater();
        LinearLayout container = (LinearLayout) findViewById(R.id.content_layout);
        inflater.inflate(R.layout.main_layout, container);

        // Set the adapter for the list view
        mDrawerList.setAdapter(new ArrayAdapter<String>(this,
                R.layout.drawer_list_item, choices));

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // Enable immersive mode to get the most out of the available screen space
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        map.onCreate();
        gps.onCreate();

        camera.onCreate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        map.setUpMapIfNeeded();
        camera.onResume();

    }
    
    @Override
    public void onPause() {
        super.onPause();
        camera.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        camera.onDestroy();
        gps.removeCallback();

    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            camera.cameraMode = position;
        }
    }

}
