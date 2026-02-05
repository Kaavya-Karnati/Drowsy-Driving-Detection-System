package com.example.drowsydrivingdetection;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class ModelPage extends AppCompatActivity {
    private static final int CAMERA_CODE = 100; //distinguishing camera permissions from audio, storage, etc

    private PreviewView previewView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model);

        previewView = findViewById(R.id.cameraView);

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
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();

                preview.setSurfaceProvider(previewView.getSurfaceProvider()); //connecting the mobile camera to previewView or the screen

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll(); //clear old camera uses (better to do to prevent crashes)

                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview
                ); //camera starts automatically and stops when activity is destroyed (no manual clean up to close the activity/camera needed)

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Camera failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show(); //display the reason camera didn't start
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(reqCode, permissions, grantResults);

        if (reqCode == CAMERA_CODE) {
            if (grantResults.length > 0 & grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera(); //start camera since permission was granted by user
            } else {
                Toast.makeText(this, "Camera permission is required to start detection", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}
