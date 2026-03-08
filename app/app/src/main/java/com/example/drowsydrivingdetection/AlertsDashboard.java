package com.example.drowsydrivingdetection;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlertsDashboard extends NavActivity{

    private TextView audioAlertDetectionText;
    private TextView visualAlertDetectionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_records);

        audioAlertDetectionText = findViewById(R.id.AuditoryAlertTotal);
        visualAlertDetectionText = findViewById(R.id.VisualAlertTotal);

        runOnUiThread(() -> {
            audioAlertDetectionText.setText(getCurrentAlertCounts() + " alerts.");
        });

        setupBottomNavigation();
    }

    private int getCurrentAlertCounts(){
        int currentAlerts = sharedPreferences.getInt("audio_alert", 0);

        return currentAlerts;
    }

}
