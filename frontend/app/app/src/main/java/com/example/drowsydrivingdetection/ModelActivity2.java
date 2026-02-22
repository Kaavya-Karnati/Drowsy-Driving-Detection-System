package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ModelActivity2 extends NavActivity {

    private Button btnWakeUp;
    private Button btnStart;
    private Button btnStop;
    private Button OpenCVModel;

    private View indicatorGreen;
    private View indicatorRed;
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

        indicatorRed.setVisibility(View.VISIBLE);
        indicatorGreen.setVisibility(View.INVISIBLE);

        setupBottomNavigation();
        setupButtonListeners();
    }

    public void openCamera(View view) {
        startActivity(new Intent(this, ModelPage.class));
    }

    public void openAlert(View view) {
        startActivity(new Intent(this, AlertActivity.class));
    }


    private void setupButtonListeners() {
        btnWakeUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ModelActivity2.this, "Wake Up Alert Triggered!", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ModelActivity2.this, "Drowsy Detection Started", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(ModelActivity2.this, "Drowsy Detection Stopped", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}