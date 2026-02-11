package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class NavActivity extends AppCompatActivity {
    protected ImageView homeLogo;
    protected ImageView modelLogo;
    protected FrameLayout faqLogo;
    protected ImageView profileLogo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        if (!(this instanceof ProfileActivity)) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        }
    }
}
