package com.example.drowsydrivingdetection.ui.pages;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.drowsydrivingdetection.core.CleanDetectionResult;
import com.example.drowsydrivingdetection.core.DrowsinessTracker;
import com.example.drowsydrivingdetection.inference.ModelLoader;
import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.inference.YOLODetector;
import com.example.drowsydrivingdetection.voice.VoiceFeedbackManager;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModelPage extends AppCompatActivity implements VoiceFeedbackManager.Host {
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
    private int frameCount = 0;
    private long fpsStartTime = 0;
    private ModelLoader modelLoader;

    private YOLODetector yoloDetector;
    private DrowsinessTracker drowsinessTracker;
    private boolean audioAlertTriggered = false;
    private boolean visualAlertTriggered = false;

    //Kaavya
    // Alert overlay views and handlers for managing visual alert timing and dismissal
    private TextView visualAlertPopUp;
    private ImageView breakPopUp;
    private ImageView closePopup;
    private MediaPlayer mediaPlayer;
    private Handler visualAlertHandler = new Handler(Looper.getMainLooper());
    private Handler breakPopupHandler = new Handler(Looper.getMainLooper());

    private VoiceFeedbackManager voiceFeedbackManager;

    @Override
    public void onRequestPermissionsResult(int reqCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(reqCode, permissions, grantResults);

        //request permissions (similar to camera permission logic)
        if (reqCode == VoiceFeedbackManager.REQUEST_RECORD_AUDIO) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (voiceFeedbackManager != null) {
                voiceFeedbackManager.onRequestPermissionsResult(reqCode, granted);
            }
            return;
        }

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_model);

        previewView = findViewById(R.id.cameraView);
        capturedView = findViewById(R.id.capturedView);
        statusText = findViewById(R.id.statusText);
        fps = findViewById(R.id.fps);

        // Kaavya
        // bind alert overlay views from activity_model.xml and set X button to dismiss the break popup
        visualAlertPopUp = findViewById(R.id.visualAlertPopUp);
        breakPopUp = findViewById(R.id.breakPopUp);
        closePopup = findViewById(R.id.closePopup);
        closePopup.setOnClickListener(v -> closeBreakPopup());

        //Nirav Handles UI changes to incorporate the new changes (feedback overlay UI)
        voiceFeedbackManager = new VoiceFeedbackManager(this);
        voiceFeedbackManager.bindUi(
                findViewById(R.id.voiceFeedbackOverlay),
                findViewById(R.id.voiceFeedbackStatus),
                findViewById(R.id.btnVoiceYes),
                findViewById(R.id.btnVoiceGetHelp),
                findViewById(R.id.voiceEscalationCountdown),
                findViewById(R.id.btnVoiceCancelEmergency)
        );
        findViewById(R.id.btnTestVoiceFeedback).setOnClickListener(v -> {
            if (voiceFeedbackManager != null) {
                voiceFeedbackManager.startManualTestSession();
            }
        });
        //end Nirav
        this.drowsinessTracker = new DrowsinessTracker(getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE));

        cameraExecutor = Executors.newSingleThreadExecutor(); //create a background executor
        //dedicated to camera frame processing (different thread for better performance)

        this.loadModel();

        if (this.hasCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        //added in order to shutdown the background thread separate from the activity thread

        if (modelLoader != null) {
            modelLoader.close();
        }

        // Kaavya
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        visualAlertHandler.removeCallbacksAndMessages(null);
        breakPopupHandler.removeCallbacksAndMessages(null);
        if (voiceFeedbackManager != null) {
            voiceFeedbackManager.destroy();
            voiceFeedbackManager = null;
        }
        //destroying the voice feedback flow
        drowsinessTracker.reset();
    }


    @Override
    public AppCompatActivity getActivity() {
        return this;
    }

    @Override
    public SharedPreferences getPrefs() {
        return getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
    }

    @Override
    public void stopAlarmSoundOnly() {
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                } catch (Exception ignored) {
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        });
    }

    @Override
    public boolean hasRecordAudioPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void requestRecordAudioPermission(int requestCode) {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.RECORD_AUDIO},
                requestCode
        );
    }
    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.CAMERA},
                CAMERA_CODE
        );
    }

    private void startCamera() {
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
                imageAnalysis.setAnalyzer(cameraExecutor, image -> {
                    try {
                        processFrame(image);
                    } catch (Exception err) {
                        Log.e("ImageAnalysis", "Frame processing error", err);
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
                Log.e("CameraX", "Camera failed", e);
                Toast.makeText(this, "Camera error", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }


    @SuppressLint("UnsafeOptInUsageError")
    private void processFrame(ImageProxy img) {
        //Convert ImageProxy obj -> Bitmap (used primarily for ml models)
        Bitmap bitmap = imageProxyToBitmap(img);

        if (bitmap != null) {
            CleanDetectionResult result = yoloDetector.detectAndParse(bitmap);
            drowsinessTracker.addFrame(result);
            checkAndTriggerAlerts();

            updateFPSCounter();

            runOnUiThread(() -> {
                capturedView.setImageBitmap(bitmap);
                String info = result.toString() + "\n" + drowsinessTracker.getDrowsinessLevel();
                statusText.setText(info);
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
        // Rotate images to ensure model receives good oriented frames (front camera is often mirrored)
        if (rotationDegrees == 0) return bitmap;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    private void updateFPSCounter() {
        frameCount++;
        long now = System.currentTimeMillis();

        if (fpsStartTime == 0) {
            fpsStartTime = now;
        }

        long elapsed = now - fpsStartTime;

        if (elapsed >= 1000) {
            int calculatedFps = (int) (frameCount * 1000.0 / elapsed);
            String fpsText = "FPS: " + calculatedFps;
            runOnUiThread(() -> fps.setText(fpsText));

            frameCount = 0;
            fpsStartTime = now;
        }

        //gives the real time processing FPS
    }


    private void loadModel() {
        final String modelLoadingText = "Loading model";
        this.statusText.setText(modelLoadingText);

        new Thread(() -> {
            modelLoader = new ModelLoader(this);
            final String modelLoadedText = "Model Loaded";

            runOnUiThread(() -> {
                if (modelLoader.isLoaded()) {
                    statusText.setText(modelLoadedText);
                    yoloDetector = new YOLODetector(
                            modelLoader.getInterpreter(),
                            modelLoader.getLabels(),
                            modelLoader.getInputSize()
                    );
                    Log.d("Model ready: ", String.valueOf(modelLoader.getLabels().size()));
                } else {
                    final String modelFailedLoadText = "Model failed to load";
                    statusText.setText(modelFailedLoadText);
                }
            });
        }).start();
    }

    private void checkAndTriggerAlerts() {
        //check for possibility of audio alert (eyes closed for 3 or more seconds)
        if (drowsinessTracker.shouldTriggerAudioAlert() && !audioAlertTriggered) {
            audioAlertTriggered = true;
            drowsinessTracker.saveAudioAlertCount();

            if (voiceFeedbackManager != null) {
                voiceFeedbackManager.notifyDrowsinessAlertFired(true);
            }
            //avoid starting the looping chime if voice check-in took over
            if (voiceFeedbackManager == null || !voiceFeedbackManager.isVoiceSessionActive()) {
                triggerAudioAlert();
            }

        } else if (!drowsinessTracker.shouldTriggerAudioAlert() && audioAlertTriggered) {
            //if eyes open again before the 3 second window, we dismiss the alert
            audioAlertTriggered = false;
            stopAudioAlert();
            Log.d("TAG", "Audio alert dismissed since eyes opened");
        }

        //check for visual alert (3 yawns) Tentative for now, could be reduced/increased
        if (drowsinessTracker.shouldTriggerVisualAlert() && !visualAlertTriggered) {
            visualAlertTriggered = true;
            drowsinessTracker.saveVisualAlertCount();
            if (voiceFeedbackManager != null) {
                voiceFeedbackManager.notifyDrowsinessAlertFired(false);
            }
            if (voiceFeedbackManager == null || !voiceFeedbackManager.isVoiceSessionActive()) {
                triggerVisualAlert();
            }
            drowsinessTracker.resetYawns();
            //resetting yawns after a visual alert is triggered

            //auto-reset visual alert after showing
            //(so it can trigger again later)
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                visualAlertTriggered = false;
            }, 5000);  // Reset after 5 seconds
        }
    }

    private void stopAudioAlert() {
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                } catch (Exception ignored) {}
                mediaPlayer.release();
                mediaPlayer = null;
            }
            // Fade out the WAKE UP banner
            visualAlertHandler.removeCallbacksAndMessages(null);
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(visualAlertPopUp, "alpha", visualAlertPopUp.getAlpha(), 0f);
            fadeOut.setDuration(400);
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    visualAlertPopUp.setVisibility(View.GONE);
                }
            });
            fadeOut.start();
        });
    }

    // Kaavya: plays audio alert and fades in the WAKE UP banner for 3 seconds when eyes have been closed >= 3s
    private void triggerAudioAlert() {
        Log.e("AUDIO ALERT", "AUDIO ALERT");
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }
            try {
                mediaPlayer = MediaPlayer.create(this, R.raw.chime_final);
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Show WAKE UP banner (stays visible while eyes remain closed)
            visualAlertHandler.removeCallbacksAndMessages(null);
            visualAlertPopUp.setAlpha(0f);
            visualAlertPopUp.setVisibility(View.VISIBLE);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(visualAlertPopUp, "alpha", 0f, 1f);
            fadeIn.setDuration(400);
            fadeIn.start();
        });
    }

    // Kaavya: fades in the break reminder popup with a dismiss button when 3+ yawns are detected
    private void triggerVisualAlert() {
        Log.e("VISUAL ALERT", "VISUAL ALERT");
        runOnUiThread(() -> {
            breakPopupHandler.removeCallbacksAndMessages(null);
            breakPopUp.setAlpha(0f);
            breakPopUp.setVisibility(View.VISIBLE);
            closePopup.setVisibility(View.VISIBLE);

            ObjectAnimator fadeIn = ObjectAnimator.ofFloat(breakPopUp, "alpha", 0f, 1f);
            fadeIn.setDuration(400);
            fadeIn.start();
        });
    }

    // Kaavya: instantly hides the break popup and dismiss button when the driver taps X
    private void closeBreakPopup() {
        breakPopUp.setVisibility(View.GONE);
        closePopup.setVisibility(View.GONE);
    }
}
