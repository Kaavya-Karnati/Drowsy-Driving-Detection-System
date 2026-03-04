package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.imageview.ShapeableImageView;

public class NavActivity extends AppCompatActivity {
    protected ImageView homeLogo;
    protected ImageView modelLogo;
    protected FrameLayout faqLogo;
    protected ShapeableImageView profileLogo;

    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile picture every time activity becomes visible
        if (profileLogo != null) {
            loadProfilePictureInNavBar();
        }
    }

    protected void setupBottomNavigation() {
        homeLogo = findViewById(R.id.homeLogo);
        modelLogo = findViewById(R.id.modelLogo);
        faqLogo = findViewById(R.id.faqLogo);
        profileLogo = findViewById(R.id.profileLogo);

        homeLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToHome();
            }
        });

        modelLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToModel();
            }
        });

        faqLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToFaq();
            }
        });

        profileLogo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToProfile();
            }
        });

        loadProfilePictureInNavBar();
    }

    private void navigateToHome() {
        if (!(this instanceof HomeActivity)) {
            Intent intent = new Intent(this, HomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void navigateToModel() {
        if (!(this instanceof ModelActivity2)) {
            Intent intent = new Intent(this, ModelActivity2.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void navigateToFaq() {
        if (!(this instanceof FaqActivity)) {
            Intent intent = new Intent(this, FaqActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }

    private void navigateToProfile() {
        // Ensure SharedPreferences is initialized
        if (sharedPreferences == null) {
            sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
        }

        // Check if user is registered or guest
        boolean isGuest = sharedPreferences.getBoolean("isGuest", true);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (!isLoggedIn || isGuest) {
            // Navigate to non-registered profile page
            if (!(this instanceof ProfileNonRegisteredActivity)) {
                Intent intent = new Intent(this, ProfileNonRegisteredActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        } else {
            // Navigate to registered profile page
            if (!(this instanceof ProfileActivity)) {
                Intent intent = new Intent(this, ProfileActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                overridePendingTransition(0, 0);
            }
        }
    }

    private void loadProfilePictureInNavBar() {
        String profilePhoto = sharedPreferences.getString("profilePhotoUri", null);
        if(profilePhoto != null){
            profileLogo.setImageURI(Uri.parse(profilePhoto));
        }
    }
}
