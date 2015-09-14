/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Rafael
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.os.Bundle;

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
        setContentView(R.layout.main_layout);

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

}
