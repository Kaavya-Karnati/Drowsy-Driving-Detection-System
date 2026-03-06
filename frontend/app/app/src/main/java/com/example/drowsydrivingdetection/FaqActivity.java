package com.example.drowsydrivingdetection;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.imageview.ShapeableImageView;

public class FaqActivity extends NavActivity {

    private ShapeableImageView profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.faq_page);

        profileIcon = findViewById(R.id.profileIcon);

        setupBottomNavigation();
        loadProfilePicture();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePicture();
    }

    private void loadProfilePicture() {
        String profilePhoto = sharedPreferences.getString("profilePhotoUri", null);
        if(profilePhoto != null){
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        }
    }
}