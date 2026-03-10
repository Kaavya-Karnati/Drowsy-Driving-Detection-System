package com.example.drowsydrivingdetection;

import android.os.Bundle;
import android.widget.TextView;

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
            if (getCurrentAudioAlertCounts() == 1) {
                audioAlertDetectionText.setText(getCurrentAudioAlertCounts() + " alert");
            } else {
                audioAlertDetectionText.setText(getCurrentAudioAlertCounts() + " alerts");
            }

            if (getCurrentVisualAlertCounts() == 1){
                visualAlertDetectionText.setText(getCurrentVisualAlertCounts() + " alert");
            } else {
                visualAlertDetectionText.setText(getCurrentVisualAlertCounts() + " alerts");
            }
        });

        setupBottomNavigation();
    }

    private int getCurrentAudioAlertCounts(){
        int currentAudioAlerts = sharedPreferences.getInt("audio_alert", 0);

        return currentAudioAlerts;
    }

    private int getCurrentVisualAlertCounts(){
        int currentVisualAlerts = sharedPreferences.getInt("visual_alert", 0);

        return currentVisualAlerts;
    }

}
