/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 *
 * Modulo encargado de verificar si se ha cometido alguna infracción
 */

package com.example.uemcar;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Infraccion {

    Activity activity;

    public int signs[] = new int[5];  // Señales encontradas
    private boolean girandoDcha[] = new boolean[2];
    private boolean girandoIzq[] = new boolean[2];

    int lastAzimuth;  // Brujula
    private TextView t1, t2;  // Texto de la derecha y la izquierda de la pantalla
    private Handler handler;  // Handler para los threads/runnables

    // Array donde se guardan las ultimas 5 velocidades registradas para poder luego hacer la media
    private List<Float> lastSpeeds = new ArrayList<>();

    public Infraccion(final Activity act) {
        activity = act;
    }

    public void setUp() {
        for(int i = 0; i < 5; i++)  // Ponemos a 0 las velocidades
            lastSpeeds.add((float) 0);

        handler = new Handler();
        t1 = (TextView) activity.findViewById(R.id.textView);
        t2 = (TextView) activity.findViewById(R.id.textView2);
        handler.postDelayed(runnable, 2000);  // Velocidad y infracciones actualizadas cada 2 segundos
        handler.postDelayed(runnable2, 5000);  // Comprobar si hemos girado cada 5 segundos
    }

    /**
     * Comprobamos cada 2 segundos si se ha cometido alguna infracción en base a las
     * señales detectadas
     * Actualizamos velocidad y texto
     */
    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            float speed = ((((MainActivity) activity).gps.mLastLocation.getSpeed()*3600)/1000);
            lastSpeeds.remove(4);  // Quitamos la velocidad mas vieja
            lastSpeeds.add(0, speed);  // Añadimos la nueva

            if (speed > 120) {  // No se puede ir a mas de 120 Km/h
                ((MainActivity) activity).map.putMarker
                        (((MainActivity) activity).gps.mLastLocation); //Tightly coupled
                escribirInfraccion("Velocidad superior a 120");
                ((MainActivity) activity).camera.storePhoto
                        (getCurrentTimeStamp() + " Velocidad");
            }

            String si = "\n";
            String s2 = "Velocidad:" + Float.toString(speed) + "\nAcimut: " +
                    ((MainActivity) activity).gps.azimuth + "°";

            if (girandoIzq[0])
                s2 += "\nGirando a la izquierda";
            else if (girandoDcha[0])
                s2 += "\nGirando a la derecha";

            // Señales encontradas
            for (int i = 0; i < 5; ++i) {
                if(signs[i] == 130)
                    si += "Prohibido girar a la derecha\n";
                else if(signs[i] == 140)
                    si += "Prohibido girar a la izquierda\n";
                else if(signs[i] == 150)
                    si += "STOP\n";
                else if(signs[i] != 1)
                    si += "Velocidad máxima de " + signs[i] + " Km/h\n";
            }

            t1.setText(s2);
            t2.setText("Señales detectadas:" + si);
            handler.postDelayed(runnable, 2000);

            detectarInfracciones();
        }
    };

    /**
     * Si hemos detectado alguna señal comprobamos 5 segundos
     * despues si se ha cometido alguna infraccion
     */
    private void detectarInfracciones() {
        // Infracciones de limite de velocidad haciendo la media
        for (int i =0; i < 5; ++i) {
            if(signs[i] != 1 && signs[i] != 130 && signs[i] != 140 && signs[i] != 150) {
                handler.postDelayed(new RunnableArg(signs[i]) {
                    @Override
                    public void run() {
                        float mSpeed = 0;
                        for (int i = 0; i < 5; i++) {
                            mSpeed += lastSpeeds.get(i);
                        }
                        mSpeed /= 5;
                        if (mSpeed > vel) {
                            ((MainActivity) activity).map.putMarkerHere();
                            escribirInfraccion("Velocidad superior a " + Float.toString(vel));
                            ((MainActivity) activity).camera.storePhoto
                                    (getCurrentTimeStamp() + " Velocidad");
                        }
                    }
                }, 5000);
            }

            // Detecion prohibido girar a la derecha
            else if (signs[i] == 130) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (girandoDcha[0] || girandoDcha[1]) {
                            ((MainActivity) activity).map.putMarkerHere();
                            escribirInfraccion("Giro a la derecha prohibido ");
                            ((MainActivity) activity).camera.storePhoto
                                    (getCurrentTimeStamp() + " GiroDcha");
                        }
                    }
                }, 5000);
            }

            // Detecion prohibido girar a la izquierda
            else if (signs[i] == 140) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (girandoIzq[0] || girandoIzq[1]) {
                            ((MainActivity) activity).map.putMarkerHere();
                            escribirInfraccion("Giro a la izquierda prohibido ");
                            ((MainActivity) activity).camera.storePhoto
                                    (getCurrentTimeStamp() + " GiroIzda");
                        }
                    }
                }, 5000);
            }

            // Deteccion infraccion STOP
            else if (signs[i] == 150) {
                handler.postDelayed(new RunnableArg(signs[i]) {
                    @Override
                    public void run() {
                        for (int i = 0; i < 5; i++) {
                            ((MainActivity) activity).map.putMarkerHere();
                            escribirInfraccion("Saltado STOP " + Float.toString(vel));
                            if (lastSpeeds.get(i) < 10) {
                                return;
                            }
                            ((MainActivity) activity).map.putMarkerHere();
                            escribirInfraccion("Saltado STOP " + Float.toString(vel));
                            ((MainActivity) activity).camera.storePhoto
                                    (getCurrentTimeStamp() + " STOP");
                        }
                    }
                }, 5000);
            }
        }
    }

    /**
     * Comprobamos cada 5 segundos si el terminal ha girado a la derecha/izquierda
     */
    private Runnable runnable2 = new Runnable() {
        @Override
        public void run() {
            girandoDcha[1] = girandoDcha[0];
            girandoIzq[1] = girandoIzq[0];

            int ang = (lastAzimuth - ((MainActivity) activity).gps.azimuth + 360) % 360;

            if (ang > 180 && ang < 310) {
                girandoDcha[0] = true;
                girandoIzq[0] = false;
            }
            else if (ang < 180 && ang > 50) {
                girandoIzq[0] = true;
                girandoDcha[0] = false;
            }
            else {
                girandoDcha[0] = false;
                girandoIzq[0] = false;
            }
            lastAzimuth = ((MainActivity) activity).gps.azimuth;

            handler.postDelayed(runnable2, 5000);
        }
    };

    public void removeCallback() {
        handler.removeCallbacks(runnable);
    }

    public static String getCurrentTimeStamp(){
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateFormat.format(new Date()); // Find todays date
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void escribirInfraccion(String infraccion) {
        String storageDirectory = Environment.getExternalStorageDirectory().toString();
        File f = new File(storageDirectory, "Infracciones.txt");
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(f, true));
            writer.println(getCurrentTimeStamp());
            writer.println("Latitude: " +
                    ((MainActivity) activity).gps.mLastLocation.getLatitude());
            writer.println("Longitude: " +
                    ((MainActivity) activity).gps.mLastLocation.getLongitude());
            writer.println(infraccion);
            writer.println();
            writer.close();
        } catch(java.io.IOException e){
            e.printStackTrace();
        }
    }

}
