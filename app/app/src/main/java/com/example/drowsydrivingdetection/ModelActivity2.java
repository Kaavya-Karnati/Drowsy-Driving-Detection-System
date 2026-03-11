package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import com.google.android.material.button.MaterialButton;

public class ModelActivity2 extends NavActivity {

    private MaterialButton btnStart;
    private MaterialButton btnStop;
    private View indicatorGreen;
    private View indicatorRed;
    private boolean isDetectionRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.model_ui);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
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

    public void openCV(View view) {
        startActivity(new Intent(this, OpenCV.class));
    }

    public void openAlert(View view) {
        startActivity(new Intent(this, AlertActivity.class));
    }

    private void setupButtonListeners() {

        btnStart.setOnClickListener(v -> {
            if (!isDetectionRunning) {
                isDetectionRunning = true;
                indicatorRed.setVisibility(View.INVISIBLE);
                indicatorGreen.setVisibility(View.VISIBLE);
                startActivity(new Intent(ModelActivity2.this, ModelPage.class));
                Toast.makeText(ModelActivity2.this, "Drowsy Detection Started", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            if (isDetectionRunning) {
                isDetectionRunning = false;
                indicatorGreen.setVisibility(View.INVISIBLE);
                indicatorRed.setVisibility(View.VISIBLE);
                Toast.makeText(ModelActivity2.this, "Drowsy Detection Stopped", Toast.LENGTH_SHORT).show();
            }
        });
    }
}