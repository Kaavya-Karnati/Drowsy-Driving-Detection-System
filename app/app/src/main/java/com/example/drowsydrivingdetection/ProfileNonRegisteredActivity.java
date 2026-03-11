package com.example.drowsydrivingdetection;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

public class ProfileNonRegisteredActivity extends NavActivity {

    private Button btnSaveContactInfo;
    private Button btnChangeAlertPreferences;
    private Button btnMonitorData;
    private Button btnRegisterHere;
    private Button btnLogoutGuest;
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
        btnLogoutGuest = findViewById(R.id.btnLogoutGuest);
    }

    private void setupListeners() {
        btnSaveContactInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileNonRegisteredActivity.this, "Please register to save contact info", Toast.LENGTH_SHORT).show();
//                showError("Please register to save contact info");
            }
        });

        btnChangeAlertPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileNonRegisteredActivity.this, "Please register to change alert preferences", Toast.LENGTH_SHORT).show();
//                showError("Please register to change alert preferences");
            }
        });

        btnMonitorData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileNonRegisteredActivity.this, "Please register to monitor your data", Toast.LENGTH_SHORT).show();
//                showError("Please register to monitor your data");
            }
        });

        btnRegisterHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToRegistration();
            }
        });

        btnLogoutGuest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });
    }

    // Ahmed's Code
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
    // End of Ahmed's Code

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout? You are currently using guest mode.")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        handleLogout();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void handleLogout() {
        // Clear guest session
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove("isLoggedIn");
        editor.remove("isGuest");
        editor.remove("userEmail");
        editor.remove("userName");
        editor.remove("userFirstName");
        editor.remove("userLastName");
        editor.apply();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to sign-in page
        Intent intent = new Intent(ProfileNonRegisteredActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
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