package com.example.drowsydrivingdetection.core;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// OpenCV imports
import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.inference.ModelLoader;
import com.example.drowsydrivingdetection.inference.YOLODetector;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

// Tensorflow imports
import org.tensorflow.lite.gpu.GpuDelegate;

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
    private TextView visualAlertPopUp;

    // Helper function for loading model (https://blog.tensorflow.org/2018/03/using-tensorflow-lite-on-android.html)
    private MappedByteBuffer loadModelFile(String modelName) throws IOException {
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

    private YOLODetector yoloDetector;
    private DrowsinessTracker drowsinessTracker;


    //Ahmed: true if text sent
    private boolean emergencyTTriggered = false;
    //Ahmed: when eyes are closed
    private long closed = 0;
    //Ahmed: 10 seconds
    private static final long timer = 10000;


    private MediaPlayer mediaPlayer;
    private boolean audioAlertTriggered = false;
    private boolean visualAlertTriggered = false;
    private final Handler visualAlertResetHandler = new Handler(Looper.getMainLooper());
    private final Handler visualAlertBannerHandler = new Handler(Looper.getMainLooper());

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
    private static final int ALL_PERMISSION = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        //System.out.println("total cores:" + availableThreads);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        // Initialize TFLite using ModelLoader to get proper classes
        modelLoader = new ModelLoader(this);

        if (!modelLoader.isLoaded()) {
            Log.e(TAG, "Model failed to load.");
            /*
            Bring up with Ahmed to possibly add a warning message here that the model failed to load
            instead of just logging to console -Anthony
             */
        } else {
            try {
                TFLiteSetup();

                // Initialize YOLO + drowsiness tracker after tensor sizes are known.
                yoloDetector = new YOLODetector(
                        modelLoader.getInterpreter(),
                        modelLoader.getLabels(),
                        imageW
                );
                drowsinessTracker = new DrowsinessTracker(sharedPreferences);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // OpenCV loader
        if (OpenCVLoader.initLocal()) {
            Log.d(TAG, "OpenCV successfully loaded.");
        } else {
            Log.e(TAG, "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // Keeps screen on whenever camera is in view
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_cameraview);

        // Sets detectionText to proper id in activity_cameraview.xml
        detectionText = findViewById(R.id.detectionText);
        visualAlertPopUp = findViewById(R.id.visualAlertPopUp);

        /*
        1. Sets OpenCVCamera to my_camera in camera_page.xml
        2. Checks for camera permissions
        3. Sets visibility and camera to proper listener
         */
        OpenCVCamera = findViewById(R.id.my_camera);
        checkPermissions();
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

    //Ahmed: replaced old one that only checked camera permission, this checks camera, text, location permissions
    private void checkPermissions() {
        String[] permissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };

        for (String permission : permissions) {
            if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(permissions, ALL_PERMISSION);
                return;
            }
        }

        OpenCVCamera.setCameraPermissionGranted();
        OpenCVCamera.enableView();
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
        if (requestCode == ALL_PERMISSION) {
            boolean grantCamera = false;
            boolean grantText = false;

            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CAMERA) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantCamera = true;
                }
                if (permissions[i].equals(Manifest.permission.SEND_SMS) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    grantText = true;
                }
            }

            if (grantCamera) {
                OpenCVCamera.setCameraPermissionGranted();
                OpenCVCamera.enableView();
            } else {
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_LONG).show();
                finish();
            }

            if (grantText) {
                //Ahmed: sends text if permission is granted
                triggerText();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override // If camera is not in focused view (aka if you minimize the app), disable camera
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause: ");
        if (OpenCVCamera != null) {
            OpenCVCamera.disableView();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume: ");
        if (OpenCVCamera != null && checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            OpenCVCamera.enableView();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: ");
        if (OpenCVCamera != null) {
            OpenCVCamera.disableView();
        }

        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }
            mediaPlayer = null;
        }

        visualAlertBannerHandler.removeCallbacksAndMessages(null);
        if (visualAlertPopUp != null) {
            visualAlertPopUp.setVisibility(View.GONE);
            visualAlertPopUp.setAlpha(0f);
        }

        if (drowsinessTracker != null) {
            drowsinessTracker.reset();
        }

        if (modelLoader != null) {
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

            if (yoloDetector != null && drowsinessTracker != null) {
                CleanDetectionResult result = yoloDetector.detectAndParse(imageInputBuffer, imageW, imageH);
                drowsinessTracker.addFrame(result);

                //Ahmed: emergency text check
                if (drowsinessTracker.shouldTriggerAudioAlert()) {
                    if (closed == 0) {
                        closed = System.currentTimeMillis();
                    }
                    long duration = System.currentTimeMillis() - closed;

                    if (duration >= timer && !emergencyTTriggered) {
                        emergencyTTriggered = true;
                        triggerText();
                    }
                }
                else {
                    closed = 0;
                    emergencyTTriggered = false;
                }

                checkAndTriggerAlerts();

                runOnUiThread(() -> {
                    detectionText.setText(
                            "Driver is currently: " + drowsinessTracker.getDrowsinessLevel()
                    );
                });
            }
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

    //send emergency text, got from when I did it in the ModelPage.java in Ahmed-3 branch
    private void triggerText() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Emergency text message triggered because eyes were closed too long!", Toast.LENGTH_LONG).show();
        });
        try {
            sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
            String email = sharedPreferences.getString("userEmail", null);
            String phone = sharedPreferences.getString("emergencyContact" + email, null);

            if (phone == null || phone.isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "No emergency contact", Toast.LENGTH_SHORT).show();
                });
                return;
            }

            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, 101);
                return;
            }
            android.location.LocationManager lm = (android.location.LocationManager) getSystemService(android.content.Context.LOCATION_SERVICE);
            android.location.Location location = null;
            if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED || androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                android.location.Location gps = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
                android.location.Location net = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
                location = gps;
                if (location == null) {
                    location = net;
                }
            }

            String message = "EMERGENCY: The driver is drowsy. Please check on them now!";

            if (location != null) {
                message += "\nLast Known Location: https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
            }
            else {
                message += "\nLocation unavailable";
            }
            android.telephony.SmsManager textM = android.telephony.SmsManager.getDefault();
            textM.sendTextMessage(phone, null, message, null, null);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void checkAndTriggerAlerts() {
        if (drowsinessTracker == null) {
            return;
        }

        if (drowsinessTracker.shouldTriggerAudioAlert() && !audioAlertTriggered) {
            audioAlertTriggered = true;
            drowsinessTracker.saveAudioAlertCount();
            triggerAudioAlert();
        } else if (!drowsinessTracker.shouldTriggerAudioAlert() && audioAlertTriggered) {
            audioAlertTriggered = false;
            stopAudioAlert();
        }

        if (drowsinessTracker.shouldTriggerVisualAlert() && !visualAlertTriggered) {
            visualAlertTriggered = true;
            drowsinessTracker.saveVisualAlertCount();
            drowsinessTracker.resetYawns();

            // We don't have the overlay views in this OpenCV layout yet; rely on detectionText.
            Log.d(TAG, "Visual alert triggered (yawn threshold).");

            visualAlertResetHandler.postDelayed(() -> visualAlertTriggered = false, 5000);
        }
    }

    private void triggerAudioAlert() {
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Exception ignored) {
                }
                mediaPlayer = null;
            }

            //Ahmed: look for the sound the user chose before and if they didn't use the default sound.
            int selectedSound = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE).getInt("selected_sound", R.raw.chime_final);
            //Ahmed: make a player to play that sound
            try {
                mediaPlayer = MediaPlayer.create(this, selectedSound);
                if (mediaPlayer != null) {
                    mediaPlayer.setLooping(true);
                    mediaPlayer.start();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to start audio alert", e);
            }

            if (visualAlertPopUp != null) {
                visualAlertBannerHandler.removeCallbacksAndMessages(null);
                visualAlertPopUp.setAlpha(0f);
                visualAlertPopUp.setVisibility(View.VISIBLE);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(visualAlertPopUp, "alpha", 0f, 1f);
                fadeIn.setDuration(400);
                fadeIn.start();
            }
        });
    }

    private void stopAudioAlert() {
        runOnUiThread(() -> {
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                } catch (Exception ignored) {
                }
                try {
                    mediaPlayer.release();
                } catch (Exception ignored) {
                }
                mediaPlayer = null;
            }

            if (visualAlertPopUp != null) {
                visualAlertBannerHandler.removeCallbacksAndMessages(null);
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(visualAlertPopUp, "alpha", visualAlertPopUp.getAlpha(), 0f);
                fadeOut.setDuration(400);
                fadeOut.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        visualAlertPopUp.setVisibility(View.GONE);
                    }
                });
                fadeOut.start();
            }
        });
    }

}