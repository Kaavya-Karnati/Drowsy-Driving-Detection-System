package com.example.drowsydrivingdetection.ui.pages;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.ui.nav.NavActivity;
import com.google.android.material.imageview.ShapeableImageView;

public class AlertsDashboard extends NavActivity {

    private TextView audioAlertDetectionText;
    private TextView visualAlertDetectionText;
    private ShapeableImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_records);

        audioAlertDetectionText = findViewById(R.id.AuditoryAlertTotal);
        visualAlertDetectionText = findViewById(R.id.VisualAlertTotal);
        profileIcon = findViewById(R.id.profileIcon);

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
        loadProfilePicture();
    }

    private int getCurrentAudioAlertCounts(){
        int currentAudioAlerts = sharedPreferences.getInt("audio_alert", 0);

        return currentAudioAlerts;
    }

    private int getCurrentVisualAlertCounts(){
        int currentVisualAlerts = sharedPreferences.getInt("visual_alert", 0);

        return currentVisualAlerts;
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePicture();
    }

    private void loadProfilePicture() {
        String profilePhoto = sharedPreferences.getString(getUserPhoto(), null);
        if (profilePhoto != null) {
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        }
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }

}
