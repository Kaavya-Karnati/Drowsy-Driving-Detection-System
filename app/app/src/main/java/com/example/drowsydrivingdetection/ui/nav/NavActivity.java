package com.example.drowsydrivingdetection.ui.nav;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.drowsydrivingdetection.ui.pages.FaqActivity;
import com.example.drowsydrivingdetection.ui.pages.ModelActivity2;
import com.example.drowsydrivingdetection.ui.pages.ProfileActivity;
import com.example.drowsydrivingdetection.ui.pages.ProfileNonRegisteredActivity;
import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.ui.pages.HomeActivity;
import com.google.android.material.imageview.ShapeableImageView;

public class NavActivity extends AppCompatActivity {

    protected ImageView homeLogo;
    protected ImageView modelLogo;
    protected FrameLayout faqLogo;
    protected ShapeableImageView profileLogo;

    //bigger clickable zones, not just icons
    protected View homeTab;
    protected View modelTab;
    protected View faqTab;
    protected View profileTab;

    protected SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload profile picture when returning to activity
        if (profileLogo != null) {
            loadProfilePictureInNavBar();
        }
        //Highlight right active tab
        updateTab();
    }

    protected void setupBottomNavigation() {
        //get tab containers, not just icons
        homeTab = findViewById(R.id.homeTab);
        modelTab = findViewById(R.id.modelTab);
        faqTab = findViewById(R.id.faqTab);
        profileTab = findViewById(R.id.profileTab);

        homeLogo = findViewById(R.id.homeLogo);
        modelLogo = findViewById(R.id.modelLogo);
        faqLogo = findViewById(R.id.faqLogo);
        profileLogo = findViewById(R.id.profileLogo);


        // Load profile picture, moved it to call it earlier
        loadProfilePictureInNavBar();

        //set click listeners are full tab now
        if (homeTab != null) {
            homeTab.setOnClickListener(v -> {
                highlightTab(homeTab);
                navigateToHome();
            });
        }

        if (modelTab != null) {
            modelTab.setOnClickListener(v -> {
                highlightTab(modelTab);
                navigateToModel();
            });
        }

        if (faqTab != null) {
            faqTab.setOnClickListener(v -> {
                highlightTab(faqTab);
                navigateToFaq();
            });
        }

        if (profileTab != null) {
            profileTab.setOnClickListener(v -> {
                highlightTab(profileTab);
                navigateToProfile();
            });
        }


        //highlight right tab when screen first loads
        updateTab();
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
        if (profileLogo == null) return;

        String profilePhoto = sharedPreferences.getString(getUserPhoto(), null);
        if (profilePhoto != null) {
            //glide replaced setImageURI from before, made it faster when moving to different pages after changing the profile photo
            Glide.with(this)
                    .load(Uri.parse(profilePhoto))
                    .placeholder(R.drawable.default_profile_icon)
                    .circleCrop()
                    .into(profileLogo);
        } else {
            profileLogo.setImageURI(null);
            profileLogo.setImageResource(R.drawable.default_profile_icon);
        }
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }

    //highlight tab and makes the active tab sharper and the others stay transparent
    protected void highlightTab(View activeTab) {
        if (homeTab != null) homeTab.setAlpha(0.4f);
        if (modelTab != null) modelTab.setAlpha(0.4f);
        if (faqTab != null) faqTab.setAlpha(0.4f);
        if (profileTab != null) profileTab.setAlpha(0.4f);
        if (activeTab != null) activeTab.setAlpha(1f);
    }

    //decides which tab should be the active tab
    protected void updateTab() {
        if (this instanceof HomeActivity) {
            highlightTab(homeTab);
        } else if (this instanceof ModelActivity2) {
            highlightTab(modelTab);
        } else if (this instanceof FaqActivity) {
            highlightTab(faqTab);
        } else if (this instanceof ProfileActivity) {
            highlightTab(profileTab);
        } else if (this instanceof ProfileNonRegisteredActivity) {
            highlightTab(profileTab);
        }
    }
}