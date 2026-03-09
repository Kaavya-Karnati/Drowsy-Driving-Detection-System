package com.example.drowsydrivingdetection;


import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;

public class HomeActivity extends NavActivity {

    private TextView alertCountNumber;
    private TextView auditoryAlertEmail;
    private TextView visualAlertEmail;
    private ShapeableImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        alertCountNumber = findViewById(R.id.alertCountNumber);
        auditoryAlertEmail = findViewById(R.id.auditoryAlertEmail);
        visualAlertEmail = findViewById(R.id.visualAlertEmail);
        profileIcon = findViewById(R.id.profileIcon);

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
        alertCountNumber.setText("45,678 Alerts");
        auditoryAlertEmail.setText("+20 % from last month");
        visualAlertEmail.setText("-5% from last month");
    }

    private void loadProfilePicture() {
        String profilePhoto = sharedPreferences.getString(getUserPhoto(), null);
        if(profilePhoto != null){
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        }
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }
}
