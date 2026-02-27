package com.example.drowsydrivingdetection;

import android.Manifest;
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
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

// Tensorflow imports
import org.tensorflow.lite.Interpreter;

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

public class cameraView extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

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

    // Interpreter and information for TFLite (most likely need to save for later since we're doing our dashboard?)
    protected Interpreter tflite;
    private int imageW = 640;
    private int imageH = 640; // defaults for our dataset
    private boolean modelStarted = false;
    private ByteBuffer imageInputBuffer;

    // TFLite buffers
    private float[][][] outputBuffer;
    private int[] outputTensor;
    private Bitmap modelBitmap;
    private Mat resizedRGBA;
    private int[] imageValues;

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

        // TFLite setup
        int[] inputTensor = tflite.getInputTensor(0).shape();
        outputTensor = tflite.getOutputTensor(0).shape();

        // Change width/height based on camera input
        imageH = inputTensor[1];
        imageW = inputTensor[2];

        // Set image buffer to store the size of the image
        imageInputBuffer = ByteBuffer.allocateDirect(4 * imageW * imageH * 3);
        imageInputBuffer.order(ByteOrder.nativeOrder());
        // https://ai.google.dev/edge/api/tflite/java/org/tensorflow/lite/Interpreter

        // Create buffer to store confident predictions
        outputBuffer = new float[outputTensor[0]][outputTensor[1]][outputTensor[2]];

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

        detectionText = findViewById(R.id.detectionText);

        // Sets OpenCVCamera to my_camera in camera_page.xml
        OpenCVCamera = findViewById(R.id.my_camera);
        getCameraPermissions();

        // Enables view nad tells camera to listen to this view (because disabled by default)
        OpenCVCamera.setVisibility(SurfaceView.VISIBLE);
        OpenCVCamera.setCvCameraViewListener(this);

        // returnToScreen();
    }

    // Convert bitmap to buffer (https://stackoverflow.com/questions/55777086/converting-bitmap-to-bytebuffer-float-in-tensorflow-lite-android)
    private void bitmapToBuffer(Bitmap bitmap, ByteBuffer buffer) {
        buffer.rewind();

        bitmap.getPixels(imageValues, 0, imageW, 0, 0, imageW, imageH);

        int pixel = 0;
        for (int i = 0; i < imageH; i++){
            for (int j = 0; j < imageW; j++){
                int value = imageValues[pixel++];

                buffer.putFloat(((value>> 16) & 0xFF) / 255.f);
                buffer.putFloat(((value>> 8) & 0xFF) / 255.f);
                buffer.putFloat((value & 0xFF) / 255.f);
            }
        }

        buffer.rewind();
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

    // Update drowsy UI based on awake or sleep
    public void updateUIAwakeOrDrowsy(float confidence) {
        String detectionTextUpdate = "TBD";
        if (confidence >= .8) {// We're shooting for 80% accuracy here
            detectionTextUpdate = "Asleep";
        } else {
            detectionTextUpdate = "Awake";
        }

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

        if (tflite != null){
            tflite.close();
            tflite = null;
        }
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public void onCameraViewStarted(int i, int i1) {
        // Create a resized RGB matrix that we can use whenever the camera is created
        // Also I don't know if we're supposed to use createBitmap or createScaledBitmap,
        // we can change it once we get the actual model post-processing working
        resizedRGBA = new Mat();
        modelBitmap = Bitmap.createBitmap(imageW, imageH, Bitmap.Config.ARGB_8888);
        imageValues = new int[imageW * imageH]; // only initialize once instead of every frame
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame cvCameraViewFrame) {
        Mat rgba = cvCameraViewFrame.rgba();

        Imgproc.resize(rgba, resizedRGBA, new Size(imageW, imageH));

        Utils.matToBitmap(resizedRGBA, modelBitmap);
        bitmapToBuffer(modelBitmap, imageInputBuffer);

        tflite.run(imageInputBuffer, outputBuffer);

        /* Keeps track of the highest confidence score from the model, then iterates
        thru the entire output buffer [0][4][i] (which is 8400)
        If the confidence score is greater than .8 (set in the updateUIAwake function)
        then it changes the onscreen text from awake to asleep

        I don't know if it does yawning yet (I need to clean this up so I can add debugging)
        but based on what Nirav sent I can prob change that to look at it (maybe that's why there's
        two shapes? Not sure)
         */
        float highestConfidenceScore = 0;
        for (int i = 0; i < outputTensor[2]; i++){
            float currentConfidenceScore = outputBuffer[0][4][i];
            if (currentConfidenceScore > highestConfidenceScore){
                highestConfidenceScore = currentConfidenceScore;
            }
        }

        updateUIAwakeOrDrowsy(highestConfidenceScore);
        return rgba;
    }

}
