package com.example.drowsydrivingdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModelPage extends AppCompatActivity {
    private static final int CAMERA_CODE = 100; //distinguishing camera permissions from audio,
    //storage, etc

    //ui elements (from activity_model.xml)
    private PreviewView previewView;
    private ImageView capturedView;
    private TextView statusText;
    private TextView fps;

    //camera executor for background processing of images
    private ExecutorService cameraExecutor;

    //Fps calculation attributes
    private long lastProcessTime = 0;
    private int frameCount = 0;
    private long fpsStartTime = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model);

        previewView = findViewById(R.id.cameraView);
        capturedView = findViewById(R.id.capturedView);
        statusText = findViewById(R.id.statusText);
        fps = findViewById(R.id.fps);

        cameraExecutor = Executors.newSingleThreadExecutor(); //create a background executor
        //dedicated to camera frame processing (different thread for better performance)

        if (this.hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
        //return true if camera permission is granted by user
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_CODE
        );
    }

    private void startCamera(){
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                //connecting the mobile camera to previewView or the screen

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                //Gives every camera frame in ImageProxy object

                //now to process each frame
                imageAnalysis.setAnalyzer(cameraExecutor, image ->  {
                    try{
                        processFrame(image);
                    } catch (Exception err) {

                    } finally {
                        image.close();
                    }
                    //cameraX pushes frames into analyze, runs on the camera executor thread set up
                    //in background, each frame is processed once at a time
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll(); //clear old camera uses(better to do to prevent crashes)

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                ); //camera starts automatically and stops when activity is destroyed
                //(no manual clean up to close the activity/camera needed)

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show(); //display the reason camera didn't start
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(reqCode, permissions, grantResults);

        if (reqCode == CAMERA_CODE) {
            if (grantResults.length > 0 & grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(); //start camera since permission was granted by user
            } else {
                Toast.makeText(this,
                        "Camera permission is required to start detection",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processFrame(ImageProxy img){
        //Convert ImageProxy obj -> Bitmap (used primarily for ml models)
        Bitmap bitmap = imageProxyToBitmap(img);

        if (bitmap != null) {
            this.calculateFPS();

            runOnUiThread(() -> {
                capturedView.setImageBitmap(bitmap);
                statusText.setText("Processing frames");
            });
        }
    }

    private Bitmap imageProxyToBitmap(ImageProxy img) {
        ImageProxy.PlaneProxy[] planes = img.getPlanes(); //camera frames are in YUV format
        ByteBuffer yBuffer = planes[0].getBuffer();
        ByteBuffer uBuffer = planes[1].getBuffer();
        ByteBuffer vBuffer = planes[2].getBuffer();

        int ySize = yBuffer.remaining();
        int uSize = uBuffer.remaining();
        int vSize = vBuffer.remaining();

        byte[] nv21 = new byte[ySize + uSize + vSize];

        // U and V are swapped
        yBuffer.get(nv21, 0, ySize);
        vBuffer.get(nv21, ySize, vSize);
        uBuffer.get(nv21, ySize + vSize, uSize);

        YuvImage yuvImage = new YuvImage(nv21, ImageFormat.NV21,
                img.getWidth(), img.getHeight(), null);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0,
                img.getWidth(), img.getHeight()), 100, out);

        byte[] imageBytes = out.toByteArray();
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        // Rotate bitmap if needed (front camera is often rotated)
        return rotateBitmap(bitmap, img.getImageInfo().getRotationDegrees());
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int rotationDegrees) {
        //need to rotate images to pass them in the right orientation to the yolo model
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void calculateFPS() {
        frameCount++;
        long currentTime = System.currentTimeMillis();

        if (fpsStartTime == 0) {
            fpsStartTime = currentTime;
        }

        long elapsedTime = currentTime - fpsStartTime;

        // Update FPS every second
        if (elapsedTime >= 1000) {
            final int fps_count = (int) (frameCount * 1000.0 / elapsedTime);
            runOnUiThread(() -> fps.setText("FPS: " + fps_count));

            // Reset counters
            frameCount = 0;
            fpsStartTime = currentTime;
        }
        //gives the real time processing FPS
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        //added in order to shutdown the background thread separate from the activity thread
    }
}
