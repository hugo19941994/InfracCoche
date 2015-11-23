/**
 * UEM 2015 - Sistema de Detección, Alerta y Registro de Infracciones de Tráfico
 * Nourdine Aliane
 * Mario Mata
 * Hugo Ferrando Seage
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import static android.os.Environment.getExternalStorageDirectory;

public class Camera implements CameraBridgeViewBase.CvCameraViewListener2 {

    private Mat                    mRgba;
    private Mat                    img;
    private CameraBridgeViewBase   mOpenCvCameraView;
    public int cameraMode;
    Activity activity;

    public Camera(Activity act){
        activity = act;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        //Bitmap image = BitmapFactory.decodeResource(activity.getResources(), R.drawable.inside100);
        //img = new Mat();
        //Utils.bitmapToMat(image, img);
        //Size sz = new Size(480,320);
        //Imgproc.resize(img, img, sz);
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


        //FindFeatures(mRgba.getNativeObjAddr(), img.getNativeObjAddr(), cameraMode);
        FindFeatures(mRgba.getNativeObjAddr(), cameraMode);
        return mRgba;
    }
    //public native void initCL();
    public native void FindFeatures(long frame, int mode);

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
        //initCL();
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView) activity.findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        cameraMode = 0;
    }

}
