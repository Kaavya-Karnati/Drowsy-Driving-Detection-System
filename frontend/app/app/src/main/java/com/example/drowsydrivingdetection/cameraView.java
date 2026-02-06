package com.example.drowsydrivingdetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// OpenCV imports
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

// Necessary if using List function but it's not necessary and we only use one camera so -Anthony


public class cameraView extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Tag for logging (Named after the class) -Anthony
    private static final String TAG = "cameraView: ";

    // Removed returnButton because you can just click back! -Anthony
    // Button returnButton;

    // OpenCV camera + variables
    private CameraBridgeViewBase OpenCVCamera;
    // Mat mRGBA;



    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);

        if (OpenCVLoader.initLocal()) {
            Log.d(TAG,"OpenCV successfully loaded.");
        } else {
            Log.e(TAG,"OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // Make sure device doesn't auto-dim because the camera is on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cameraview);

        // Sets OpenCVCamera to my_camera in camera_page.xml
        OpenCVCamera = findViewById(R.id.my_camera);
        getCameraPermissions();

        // Enables view nad tells camera to listen to this view (because disabled by default)
        OpenCVCamera.setVisibility(SurfaceView.VISIBLE);
        OpenCVCamera.setCvCameraViewListener(this);

        // returnToScreen();
    }

    // Request permission for camera (if not already accepted)
    // If you need to test, just hold down on the app and click App Info and in permissions just disable it if you already had it -Anthony
    private void getCameraPermissions() {
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){ // Check if permission is granted
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1); // If not, request it
        } else {
            OpenCVCamera.setCameraPermissionGranted(); // If it is, tell OpenCV's camera that we have permission to use it
        }
        OpenCVCamera.setCameraPermissionGranted();
    }

    /* Return button functionality (Hiding because we can just click the back button -Anthony)
    private void returnToScreen(){
        returnButton = findViewById(R.id.returnButton);

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();

            }
        });
    }

     */


    @Override // If camera is not in focused view (aka if you minimize the app), disable camera
    public void onPause()
    {
        super.onPause();
        Log.i(TAG, "onPause: ");
        if (OpenCVCamera != null){
            OpenCVCamera.disableView();
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (OpenCVCamera != null){
            OpenCVCamera.enableView();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        if (OpenCVCamera != null){
            OpenCVCamera.disableView();
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public void onCameraViewStarted(int i, int i1) {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        return cvCameraViewFrame.rgba();
    }

}
