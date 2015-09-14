package com.example.uemcar;

import android.os.Handler;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.app.Activity;
import android.app.FragmentManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.imgproc.Imgproc;

import java.util.Random;

public class MainActivity extends Activity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, CvCameraViewListener2 {
    private GoogleApiClient mGoogleApiClient;
    public Location mLastLocation;
    private MapManager mapManager = new MapManager();
    private LocationRequest mLocationRequest;

    private int                    mViewMode;
    private Mat                    mRgba;
    private Mat                    mIntermediateMat;
    private Mat                    mGray;
    private Button button;
    private boolean started = true;

    private CameraBridgeViewBase   mOpenCvCameraView;

    static {
        //OpenCVLoader.initDebug();
    	System.loadLibrary("opencv_java3");
        System.loadLibrary("jniPart");
    }

    private Handler handler = new Handler();

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            mapManager.putMarker(mLastLocation);
            button.setText(Float.toString((mLastLocation.getSpeed()*3600)/1000));
            if(started) {
                start();
            }
        }
    };

    public void start() {
        started = true;
        handler.postDelayed(runnable, 2000);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);

        View mainl = findViewById(R.id.mainlaoyut);
        mapManager.setView(mainl);
        mapManager.setContext(this);
        mapManager.setUpMapIfNeeded();

        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Perform action on click
                mapManager.putMarker(mLastLocation);
                button.setText(Float.toString((mLastLocation.getSpeed()*3600)/1000));
            }
        });
        start();
        buildGoogleApiClient();
        mGoogleApiClient.connect();
        createLocationRequest();
        
        //OpenCVLoader.initDebug();
        
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mOpenCvCameraView = (JavaCameraView) this.findViewById(R.id.cameraView);
        mOpenCvCameraView.setCvCameraViewListener(this);
        
    }

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        //updateUI();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        // Connected to Google Play services!
        // The good stuff goes here.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation == null) {
            Toast.makeText(this, "No location detected", Toast.LENGTH_LONG).show();
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection has been interrupted.
        // Disable any UI components that depend on Google APIs
        // until onConnected() is called.
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // This callback is important for handling errors that
        // may occur while attempting to connect with Google.
        //
        // More about this in the 'Handle Connection Failures' section.
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapManager.onResume();
        mOpenCvCameraView.enableView();
        
      /*  if (OpenCVLoader.initDebug() == true){
        	System.loadLibrary("mixed_sample");
            
            mOpenCvCameraView.enableView();
        }
        else
        	System.exit(0);*/
        
        //OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }
    
    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }
    
    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
        handler.removeCallbacks(runnable);
    }

    
   /* public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    //Log.i(TAG, "OpenCV loaded successfully");

                    // Load native library after(!) OpenCV initialization
                    System.loadLibrary("mixed_sample");
                    
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };*/
    
    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
    }

    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
    	 mRgba = inputFrame.rgba();
         mGray = inputFrame.gray();
         FindFeatures(mGray.getNativeObjAddr(), mRgba.getNativeObjAddr());
        return mRgba;
    }

    public native void FindFeatures(long matAddrGr, long matAddrRgba);
    public native void FindFace(long matAddrGr, long matAddrRgba);

}
