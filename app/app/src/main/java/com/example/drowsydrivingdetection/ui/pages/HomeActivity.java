package com.example.drowsydrivingdetection.ui.pages;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

import com.example.drowsydrivingdetection.ui.nav.NavActivity;
import com.example.drowsydrivingdetection.R;
import com.google.android.material.imageview.ShapeableImageView;

public class HomeActivity extends NavActivity {

    private ShapeableImageView profileIcon;
    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_page);

        profileIcon = findViewById(R.id.profileIcon);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        setupBottomNavigation();
        loadProfilePicture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePicture();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        loadProfilePicture();
    }

    private void loadProfilePicture() {
        String profilePhoto = sharedPreferences.getString(getUserPhoto(), null);
        if (profilePhoto != null) {
            profileIcon.setImageURI(null);
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        } else {
            profileIcon.setImageURI(null);
            profileIcon.setImageResource(R.drawable.default_profile_icon);
        }
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }
}