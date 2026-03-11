package com.example.drowsydrivingdetection;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;

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
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        }
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }
}