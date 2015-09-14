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
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Camera implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;
    private CameraBridgeViewBase   mOpenCvCameraView;
    Activity activity;

    public Camera(Activity act){
        activity = act;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    @Override
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
        return mRgba;
    }

    public native void FindFeatures(long matAddrGr, long matAddrRgba);
    public native void FindFace(long matAddrGr, long matAddrRgba);

    public void onDestroy(){
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onPause(){
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onResume(){
        mOpenCvCameraView.enableView();
    }

    public void onCreate(){
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView) activity.findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

}
