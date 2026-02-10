package com.example.drowsydrivingdetection;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
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

// Tensorflow imports
import org.tensorflow.lite.Interpreter;

// Java imports
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class cameraView extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Tag for logging (Named after the class) -Anthony
    private static final String TAG = "cameraView: ";

    // Helper function for loading model (https://blog.tensorflow.org/2018/03/using-tensorflow-lite-on-android.html)
    private MappedByteBuffer loadModelFile(String modelName) throws IOException{
            AssetFileDescriptor fileDescriptor = getAssets().openFd(modelName);
            FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
            FileChannel fileChannel = inputStream.getChannel();
            long startOffset = fileDescriptor.getStartOffset();
            long declaredLength = fileDescriptor.getDeclaredLength();
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }

    // OpenCV camera + variables
    private CameraBridgeViewBase OpenCVCamera;
    // Mat mRGBA;

    // Interpreter and information for TFLite (most likely need to save for later since we're doing our dashboard?)
    protected Interpreter tflite;
    private int imageW = 640;
    private int imageH = 640; // defaults for our dataset
    private boolean modelStarted = false;
    private ByteBuffer imageInputBuffer;

    // determines how many threads device has, later this is passed into the model for maximum performance
    int availableThreads = Runtime.getRuntime().availableProcessors();

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        //System.out.println("total cores:" + availableThreads);

        // Load model
        Interpreter.Options interpreterSettings = new Interpreter.Options();
        interpreterSettings.setNumThreads(availableThreads); // increase depending on device, gotta find a function that pulls that?
        try {
            tflite = new Interpreter(loadModelFile("best_float16.tflite"), interpreterSettings); // load model, then send the 4 thread settings (and anything else we add)
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // OpenCV loader
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
