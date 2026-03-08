package com.example.drowsydrivingdetection;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// OpenCV imports
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

// Tensorflow imports
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.gpu.GpuDelegateFactory;

// Java imports
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/*
This entire file needs to be restructured (refactored?) because I've kinda just been adding and adding
without properly cleaning it up. Splitting it up will follow NFR 3.3.4 Maintainability in our SRS (which I just wrote!)
Once I get the OpenCV -> Bitmap -> TFLite -> Post Processing/extraction pipeline fixed, I will clean this up, I just would
rather have it actually working rather than cleaning up all my code just to delete it again later because it was
wrong in the first place -Anthony
 */

public class OpenCV extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    // Tag for logging (Named after the class) -Anthony
    private static final String TAG = "cameraView: ";
    private TextView detectionText;

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
    private static final int CAMERA_PERMISSION = 1;
    // Mat mRGBA;

    // Changing from original to using ModelLoader
    private ModelLoader modelLoader;
    private int imageW = 640;
    private int imageH = 640; // defaults for our dataset
    private ByteBuffer imageInputBuffer;
    private double expectedConfidenceLevel = 0.6; // Our expected confidence before triggering an alert is .6 for prototype 2
    private GpuDelegate gpuDelegate = null;

    SharedPreferences sharedPreferences;

    // TFLite buffers
    private float[][][] outputBuffer;
    private int[] outputTensor;
    private Mat resizedRGBA;
    private int[] imageValues;
    byte[] matToByteBufferArray;

    // For timer
    // Basically, instead of running tflite.run on every frame, it'll only run every 5 frames
    // My PC gets about ~4fps running on every frame vs 12-15 when running every 5 frames.
    // We can increase the number based on the amount of times
    int totalFrames = 0;
    int inferOnFrame = 5;



    // determines how many threads device has, later this is passed into the model for maximum performance
    int availableThreads = Runtime.getRuntime().availableProcessors();

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        //System.out.println("total cores:" + availableThreads);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        // Initialize TFLite using ModelLoader to get proper classes
        modelLoader = new ModelLoader(this);

        if (!modelLoader.isLoaded()){
            Log.e(TAG, "Model failed to load.");
            /*
            Bring up with Ahmed to possibly add a warning message here that the model failed to load
            instead of just logging to console -Anthony
             */
        } else {
            try {
                TFLiteSetup();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // OpenCV loader
        if (OpenCVLoader.initLocal()) {
            Log.d(TAG,"OpenCV successfully loaded.");
        } else {
            Log.e(TAG,"OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // Keeps screen on whenever camera is in view
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cameraview);

        // Sets detectionText to proper id in activity_cameraview.xml
        detectionText = findViewById(R.id.detectionText);

        /*
        1. Sets OpenCVCamera to my_camera in camera_page.xml
        2. Checks for camera permissions
        3. Sets visibility and camera to proper listener
         */
        OpenCVCamera = findViewById(R.id.my_camera);
        getCameraPermissions();
        OpenCVCamera.setVisibility(SurfaceView.VISIBLE);
        OpenCVCamera.setCvCameraViewListener(this);
    }

    private void TFLiteSetup() throws IOException {
        // TFLite setup (but using modelLoader instead)
        int[] inputTensor = modelLoader.getInterpreter().getInputTensor(0).shape();
        outputTensor = modelLoader.getInterpreter().getOutputTensor(0).shape();

        // Change width/height based on camera input
        imageH = inputTensor[1];
        imageW = inputTensor[2];

        // Set image buffer to store the size of the image
        imageInputBuffer = ByteBuffer.allocateDirect(4 * imageW * imageH * 3);
        imageInputBuffer.order(ByteOrder.nativeOrder());

        // Create buffer to store confidence predictions
        outputBuffer = new float[outputTensor[0]][outputTensor[1]][outputTensor[2]];
        // https://ai.google.dev/edge/api/tflite/java/org/tensorflow/lite/Interpreter
    }

    // Request permission for camera (if not already accepted)
    // If you need to test, just hold down on the app and click App Info and in permissions just disable it if you already had it -Anthony
    private void getCameraPermissions() {
        if(checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){ // Check if permission is granted
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION); // If not, request it
        } else {
            OpenCVCamera.setCameraPermissionGranted(); // If it is, tell OpenCV's camera that we have permission to use it
            OpenCVCamera.enableView();
        }
    }

    private void saveAlertCount() {
        // get updated alert from shared preferences
        // Most likely we will change how this works if we implement it being set by different dates
        int currentAlerts = sharedPreferences.getInt("audio_alert", 0);

        Log.d(TAG, "Saved audio alert, current count: " + currentAlerts);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("audio_alert", currentAlerts + 1);
        editor.apply();
    }

    // Update drowsy UI based on awake or sleep
    public void updateUIAwakeOrDrowsy(float confidence) {
        String detectionTextUpdate = "TBD";

        String percent = String.format("%.2f", confidence * 100);

        if (confidence >= expectedConfidenceLevel) {// We're shooting for 80% accuracy on the file, but 60% for prototype 2
            detectionTextUpdate = "Asleep (%: " + percent + ")";
            saveAlertCount();
            Log.i(TAG, detectionTextUpdate); // logging
        } else {
            detectionTextUpdate = "Awake (%: " + percent + ")";
            Log.i(TAG, detectionTextUpdate); // logging
        }

        //
        String finalDetectionTextUpdate = detectionTextUpdate;
        runOnUiThread(() -> {
            detectionText.setText("Driver is currently: " + finalDetectionTextUpdate);
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Check request code from getCameraPermissions() to see which one it is. If we implement the Google Maps API we could also change these to check for more than just camera -Anthony
        if (requestCode == CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                OpenCVCamera.setCameraPermissionGranted();
                OpenCVCamera.enableView();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
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
        if (OpenCVCamera != null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
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

        if (modelLoader != null){
            modelLoader.close();
            modelLoader = null;
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public void onCameraViewStarted(int i, int i1) {
        // Create a resized RGB matrix that we can use whenever the camera is created
        resizedRGBA = new Mat();
        matToByteBufferArray = new byte[imageW * imageH * 3];
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        Mat rgba = cvCameraViewFrame.rgba();

        if (totalFrames % inferOnFrame == 0) {
            // Resizes image and converts to proper color channels
            ImageProcessing.resizeImageConvertColor(rgba, resizedRGBA, imageW, imageH);

            // Converts mat (resizedRGBA) into imageInputBuffer
            ImageProcessing.matToByteBuffer(resizedRGBA, imageInputBuffer, matToByteBufferArray);

            // Swapped to using modelLoader instead
            modelLoader.getInterpreter().run(imageInputBuffer, outputBuffer);

            /*
            Needs to be changed to actually do proper YOLO detections
            most likely through YOLODetector
             */
            float highestConfidenceScore = 0;
            for (int i = 0; i < outputTensor[2]; i++){
                float currentConfidenceScore = outputBuffer[0][4][i];
                // Log.d(TAG, "Currently detecting: ");
                if (currentConfidenceScore > highestConfidenceScore){
                    highestConfidenceScore = currentConfidenceScore;
                }
            }

            updateUIAwakeOrDrowsy(highestConfidenceScore);
        }
        totalFrames++;

        /* Keeps track of the highest confidence score from the model, then iterates
        thru the entire output buffer [0][4][i] (which is 8400)
        If the confidence score is greater than .6 (set in the updateUIAwake function)
        then it changes the onscreen text from awake to asleep

        I don't know if it does yawning yet (I need to clean this up so I can add debugging)
        but based on what Nirav sent I can prob change that to look at it (maybe that's why there's
        two shapes? Not sure)
         */

        return rgba;
    }

}
