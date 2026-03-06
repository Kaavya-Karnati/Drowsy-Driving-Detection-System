package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class ProfileNonRegisteredActivity extends NavActivity {

    private Button btnSaveContactInfo;
    private Button btnChangeAlertPreferences;
    private Button btnMonitorData;
    private Button btnRegisterHere;
    private TextView errorBanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        initializeViews();
        setupBottomNavigation();
        setupListeners();
    }

    private void initializeViews() {
        btnSaveContactInfo = findViewById(R.id.btnSaveContactInfo);
        btnChangeAlertPreferences = findViewById(R.id.btnChangeAlertPreferences);
        btnMonitorData = findViewById(R.id.btnMonitorData);
        btnRegisterHere = findViewById(R.id.btnRegisterHere);
        errorBanner = findViewById(R.id.errorBanner);
    }

    private void setupListeners() {
        btnSaveContactInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showError("Please register to save contact info");
            }
        });

        btnChangeAlertPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showError("Please register to change alert preferences");
            }
        });

        btnMonitorData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showError("Please register to monitor your data");
            }
        });

        btnRegisterHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToRegistration();
            }
        });
    }

    private void showError(String message) {
        errorBanner.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
        errorBanner.setAlpha(0f);
        errorBanner.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        errorBanner.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                errorBanner.animate()
                                        .alpha(0f)
                                        .setDuration(300)
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                errorBanner.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }, 2000);
                    }
                });
    }

    private void navigateToRegistration() {
        // Clear guest session
        SharedPreferences sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isLoggedIn");
        editor.remove("isGuest");
        editor.apply();

        Intent intent = new Intent(ProfileNonRegisteredActivity.this, RegistrationActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}