/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

public class Infraccion {

    Activity activity;

    public Infraccion(final Activity act) {
        activity = act;
    }

    public int signs[] = new int[5];
    private boolean girandoDcha[] = new boolean[2];
    private boolean girandoIzq[] = new boolean[2];
    int lastAzimuth;
    private TextView t1, t2;
    private Handler handler;
    private List<Float> myList = new ArrayList<>();

    public void setUp() {
        for(int i = 0; i < 5; i++)
            myList.add((float) 0);
        handler = new Handler();
        t1 = (TextView) activity.findViewById(R.id.textView);
        t2 = (TextView) activity.findViewById(R.id.textView2);
        handler.postDelayed(runnable, 2000);
        handler.postDelayed(runnable2, 5000);
    }

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            float speed = ((((MainActivity) activity).gps.mLastLocation.getSpeed()*3600)/1000);
            myList.remove(4);
            myList.add(0, speed);
            if (speed > 120)
                ((MainActivity) activity).map.putMarker(((MainActivity) activity).gps.mLastLocation); //Tightly coupled

            String si = "\n";
            String s2 = "Velocidad:" + Float.toString(speed) + "\nAcimut: " + ((MainActivity) activity).gps.azimuth + "°";

            if (girandoIzq[0])
                s2 += "\nGirando a la izquierda";
            else if (girandoDcha[0])
                s2 += "\nGirando a la derecha";


            for (int i =0; i < 5; ++i) {
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

            // Detectar infr. limite de velocidad 5 segundos despues haciendo la media
            for (int i = 0; i < 5; ++i) {
                if(signs[i] != 1 && signs[i] != 130 && signs[i] != 140 && signs[i] != 150) {
                    handler.postDelayed(new RunnableArg(signs[i]) {
                        @Override
                        public void run() {
                            float mSpeed = 0;
                            for (int i = 0; i < 5; i++) {
                                Log.d("V", Float.toString(myList.get(i)));
                                mSpeed += myList.get(i);
                            }
                            mSpeed /= 5;
                            if (mSpeed > vel) {
                                ((MainActivity) activity).map.putMarkerHere();
                                escribirInfraccion("Velocidad superior a " + Float.toString(vel));
                            }

                        }
                    }, 5000);
                }

                // Detecion prohibido girar a la derecha
                else if (signs[i] == 130) {
                    handler.postDelayed(new RunnableArg(signs[i]) {
                        @Override
                        public void run() {
                            if (girandoDcha[0] || girandoDcha[1]) {
                                ((MainActivity) activity).map.putMarkerHere();
                                escribirInfraccion("Giro a la derecha prohibido ");
                            }
                        }
                    }, 5000);
                }

                // Detecion prohibido girar a la izquierda
                else if (signs[i] == 140) {
                    handler.postDelayed(new RunnableArg(signs[i]) {
                        @Override
                        public void run() {
                            if (girandoIzq[0] || girandoIzq[1]) {
                                ((MainActivity) activity).map.putMarkerHere();
                                escribirInfraccion("Giro a la izquierda prohibido ");
                            }
                        }
                    }, 5000);
                }

                // Deteccion infraccion STOP
                else if (signs[i] == 150) {
                    handler.postDelayed(new RunnableArg(signs[i]) {
                        @Override
                        public void run() {

                        }
                    }, 5000);
                }
            }
        }
    };

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
            String currentTimeStamp = dateFormat.format(new Date()); // Find todays date
            return currentTimeStamp;
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
            writer.println("Latitude: " + ((MainActivity) activity).gps.mLastLocation.getLatitude());
            writer.println("Longitude: " + ((MainActivity) activity).gps.mLastLocation.getLongitude());
            writer.println(infraccion);
            writer.println();
            writer.close();
        } catch(java.io.IOException e){
            return;
        }
    }

}
