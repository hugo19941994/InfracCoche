/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 *
 * Implementación de Runnable con un argumento
 * Comodo para hacer un thread para detectar infracciones de velocidad,
 * pasando como parametro la velocidad indicada en la señal
 */

package com.example.uemcar;

public class RunnableArg implements Runnable {
    float vel;

    public RunnableArg(float n){
        vel = n;
    }

    /**
     * Implementado en el modulo de infraccion para
     * poder acceder a las ultimas velocidades y hacer la media
     */
    @Override
    public void run() {}
}
