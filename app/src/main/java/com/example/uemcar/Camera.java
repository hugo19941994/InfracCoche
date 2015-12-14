/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Camera implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat                    mRgba;
    private CameraBridgeViewBase   mOpenCvCameraView;
    public int cameraMode;
    Activity activity;

    public Camera(Activity act) {
        activity = act;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        //mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        //mGray = inputFrame.gray();
        //int senales[];
        // Pasar a GPS las señales encontradas
        ((MainActivity) activity).infraccion.signs = FindFeatures(mRgba.getNativeObjAddr(), cameraMode);
        //((MainActivity) activity).gps.signs = FindFeatures(mRgba.getNativeObjAddr(), cameraMode);
        return mRgba;  // Frame de camera en formato seleccionado
    }

    public native int[] FindFeatures(long frame, int mode);

    public void onDestroy() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onPause() {
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onResume() {
        mOpenCvCameraView.enableView();
    }

    public void onCreate() {
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView) activity.findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        cameraMode = 0;
    }

}
