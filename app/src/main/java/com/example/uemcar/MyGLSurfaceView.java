package com.example.uemcar;


import org.opencv.android.CameraGLSurfaceView;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.TextView;
import android.widget.Toast;

public class MyGLSurfaceView extends CameraGLSurfaceView implements CameraGLSurfaceView.CameraTextureListener {

    static final String LOGTAG = "MyGLSurfaceView";
    protected int  frameCounter;
    protected long lastNanoTime;
    String mFpsText = null;

    public MyGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        super.surfaceCreated(holder);
        //NativePart.initCL();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //NativePart.closeCL();
        super.surfaceDestroyed(holder);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        ((Activity) getContext()).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getContext(), "onCameraViewStarted", Toast.LENGTH_SHORT).show();
            }
        });
        initCL();
        frameCounter = 0;
        lastNanoTime = System.nanoTime();
    }

    @Override
    public void onCameraViewStopped() {
        ((Activity) getContext()).runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getContext(), "onCameraViewStopped", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCameraTexture(int texIn, int texOut, int width, int height) {
        // FPS
        frameCounter++;
        if(frameCounter >= 30)
        {
            final int fps = (int) (frameCounter * 1e9 / (System.nanoTime() - lastNanoTime));
            Log.i(LOGTAG, "drawFrame() FPS: "+fps);
            if(mFpsText != null) {
                Runnable fpsUpdater = new Runnable() {
                    public void run() {
                        mFpsText = ("FPS: " + fps);
                    }
                };
                new Handler(Looper.getMainLooper()).post(fpsUpdater);
            } else {
                Log.d(LOGTAG, "mFpsText == null");
            }
            frameCounter = 0;
            lastNanoTime = System.nanoTime();
        }

        FindFeatures(texIn, texOut, width, height);
        return true;
    }
    public native void FindFeatures(int texIn, int texOut, int w, int h);
    public native void initCL();

}