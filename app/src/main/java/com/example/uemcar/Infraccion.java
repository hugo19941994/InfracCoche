/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.os.Handler;
import android.widget.TextView;

public class Infraccion {

    Activity activity;

    public Infraccion(final Activity act) {
        activity = act;
    }

    public int signs[] = new int[5];
    private int v10, v20, v30, v40, v50, v60, v70, v80, v90, v100, v110, v120, pGD, pGI, STOP;
    private TextView t1, t2;
    private Handler handler;

    public void setUp() {
        handler = new Handler();
        t1 = (TextView) activity.findViewById(R.id.textView);
        t2 = (TextView) activity.findViewById(R.id.textView2);
        handler.removeCallbacks(runnable);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            Float speed = ((((MainActivity) activity).gps.mLastLocation.getSpeed()*3600)/1000);
            if (speed > 120)
                ((MainActivity) activity).map.putMarker(((MainActivity) activity).gps.mLastLocation); //Tightly coupled

            String si = "\n";

            for (int i =0; i < 5; ++i) {
                if(signs[i] == 130)
                    si += "Prohibido girar a la derecha\n";
                else if(signs[i] == 140)
                    si += "Prohibido girar a la izquierda\n";
                else if(signs[i] != 1)
                    si += "Velocidad máxima de " + signs[i] + " Km/h\n";
            }

            t1.setText("Velocidad: " + Float.toString(speed) + "\nAcimut: " + ((MainActivity) activity).gps.azimuth + "°");
            t2.setText("Señales detectadas:" + si);
            handler.postDelayed(runnable, 2000);
        }
    };

    public void removeCallback() {
        handler.removeCallbacks(runnable);
    }

}
