/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
 * Rafael Mesa Hernández
 * Licencia: Attribution-NonCommercial-NoDerivatives 4.0 International
 */

package com.example.uemcar;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.WindowManager;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class Camera implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;
    private Mat                    mHLS;
    private Mat                    mContours;
    private CameraBridgeViewBase   mOpenCvCameraView;
    public int cameraMode;
    Activity activity;

    public Camera(Activity act){
        activity = act;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mHLS = new Mat(height, width, CvType.CV_8UC1);
        mContours = new Mat(height, width, CvType.CV_8UC1);
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
        Bitmap image = BitmapFactory.decodeResource(activity.getResources(), R.drawable.cien);
        Mat img = new Mat();
        Utils.bitmapToMat(image, img);
        FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr(), img.getNativeObjAddr(), mHLS.getNativeObjAddr(), mContours.getNativeObjAddr());
        switch (cameraMode) {
            case (0):
                return mRgba;
            case(1):
                return mGray;
            case(2):
                return mHLS;
            case(3):
                return mContours;
            default:
                return mRgba;
        }
    }

    public native void FindFeatures(long img, long matAddrGr, long image, long HLS, long Contours);

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
        cameraMode = 0;
    }

}
