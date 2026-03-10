package com.example.drowsydrivingdetection;


import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;

public class HomeActivity extends NavActivity {

    private TextView alertCountNumber;
    private TextView auditoryAlertEmail;
    private TextView visualAlertEmail;
    private ShapeableImageView profileIcon;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        alertCountNumber = findViewById(R.id.alertCountNumber);
        auditoryAlertEmail = findViewById(R.id.auditoryAlertEmail);
        visualAlertEmail = findViewById(R.id.visualAlertEmail);
        profileIcon = findViewById(R.id.profileIcon);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        setupBottomNavigation();
        loadHomeData();
        loadProfilePicture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePicture();
    }

    private void loadHomeData() {
        int currentAudioAlerts = sharedPreferences.getInt("audio_alert", 0);
        int currentVisualAlerts = sharedPreferences.getInt("visual_alert", 0);
        int totalAlerts = currentAudioAlerts + currentVisualAlerts;
        alertCountNumber.setText(totalAlerts + " Alerts");
        auditoryAlertEmail.setText("+20 % from last month");
        visualAlertEmail.setText("-5% from last month");
    }

    private void loadProfilePicture() {
        String profilePhoto = sharedPreferences.getString("profilePhotoUri", null);
        if(profilePhoto != null){
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        }
    }
}
