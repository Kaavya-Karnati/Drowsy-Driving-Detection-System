package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class ModelActivity2 extends NavActivity {

    private Button btnWakeUp;
    private Button btnStart;
    private Button btnStop;
    private Button OpenCVModel;

    private View indicatorGreen;
    private View indicatorRed;
    private TextView passBanner;
    private boolean isDetectionRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.model_ui);

        btnWakeUp = findViewById(R.id.btnWakeUp);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        OpenCVModel = findViewById(R.id.openCVButton);
        indicatorGreen = findViewById(R.id.statusIndicatorGreen);
        indicatorRed = findViewById(R.id.statusIndicatorRed);
        passBanner = findViewById(R.id.passBanner);

        indicatorRed.setVisibility(View.VISIBLE);
        indicatorGreen.setVisibility(View.INVISIBLE);

        setupBottomNavigation();
        setupButtonListeners();
    }

    public void openCamera(View view) {
        startActivity(new Intent(this, ModelPage.class));
    }

    private void showPass(String message) {
        passBanner.setText(message);
        passBanner.setVisibility(View.VISIBLE);
        passBanner.setAlpha(0f);
        passBanner.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        passBanner.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                passBanner.animate()
                                        .alpha(0f)
                                        .setDuration(300)
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                passBanner.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }, 2000);
                    }
                });
    }

    private void setupButtonListeners() {
        btnWakeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPass("Wake Up Alert Triggered!");
            }
        });


        OpenCVModel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // I commented this out because I couldn't get a fix working, so if it sounds like it's clicking a lot... it is -Anthony
                // view.setEnabled(false); // Disables extra clicking
                Intent cameraIntent = new Intent(ModelActivity2.this, cameraView.class);
                startActivity(cameraIntent);
            }
        });

        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isDetectionRunning) {
                    isDetectionRunning = true;
                    indicatorRed.setVisibility(View.INVISIBLE);
                    indicatorGreen.setVisibility(View.VISIBLE);
                    showPass("Drowsy Detection Started");
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isDetectionRunning) {
                    isDetectionRunning = false;
                    indicatorGreen.setVisibility(View.INVISIBLE);
                    indicatorRed.setVisibility(View.VISIBLE);
                    showPass("Drowsy Detection Stopped");
                }
            }
        });
    }
}