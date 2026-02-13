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

import com.google.android.material.switchmaterial.SwitchMaterial;

public class ProfileActivity extends NavActivity {

    private TextView userName;
    private TextView userEmail;
    private Button btnUpload;
    private Button btnChange;
    private Button btnLogout;
    private SwitchMaterial switchNarcolepsy;
    private SwitchMaterial switchSleepApnea;
    private SwitchMaterial switchInsomnia;
    private SwitchMaterial switchAuditoryAlerts;
    private SwitchMaterial switchVisualAlerts;
    private TextView auditoryAlertsCount;
    private TextView visualAlertsCount;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logged_in_profile_page);

        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        initializeViews();
        setupBottomNavigation();
        loadUserData();
        setupListeners();
    }

    private void initializeViews() {
        userName = findViewById(R.id.userName);
        userEmail = findViewById(R.id.userEmail);
        btnUpload = findViewById(R.id.btnUpload);
        btnChange = findViewById(R.id.btnChange);
        btnLogout = findViewById(R.id.btnLogout);
        switchNarcolepsy = findViewById(R.id.switchNarcolepsy);
        switchSleepApnea = findViewById(R.id.switchSleepApnea);
        switchInsomnia = findViewById(R.id.switchInsomnia);
        switchAuditoryAlerts = findViewById(R.id.switchAuditoryAlerts);
        switchVisualAlerts = findViewById(R.id.switchVisualAlerts);
        auditoryAlertsCount = findViewById(R.id.auditoryAlertsCount);
        visualAlertsCount = findViewById(R.id.visualAlertsCount);
    }

    private void loadUserData() {
        // Load user data from SharedPreferences
        String firstName = sharedPreferences.getString("userFirstName", "");
        String lastName = sharedPreferences.getString("userLastName", "");
        String fullName = sharedPreferences.getString("userName", firstName + " " + lastName);
        userName.setText(fullName);
        userEmail.setText(sharedPreferences.getString("userEmail", "default@gmail.com"));

        // Load switch states
        switchNarcolepsy.setChecked(sharedPreferences.getBoolean("narcolepsy", false));
        switchSleepApnea.setChecked(sharedPreferences.getBoolean("sleepApnea", false));
        switchInsomnia.setChecked(sharedPreferences.getBoolean("insomnia", false));
        switchAuditoryAlerts.setChecked(sharedPreferences.getBoolean("auditoryAlerts", true));
        switchVisualAlerts.setChecked(sharedPreferences.getBoolean("visualAlerts", true));

        // Load alert counts
        auditoryAlertsCount.setText(sharedPreferences.getString("auditoryCount", "00"));
        visualAlertsCount.setText(sharedPreferences.getString("visualCount", "--"));
    }

    private void setupListeners() {
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "Upload profile picture", Toast.LENGTH_SHORT).show();
            }
        });

        btnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileActivity.this, "Change profile picture", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLogoutDialog();
            }
        });


        // Save switch states when changed
        switchNarcolepsy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("narcolepsy", isChecked).apply();
        });

        switchSleepApnea.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("sleepApnea", isChecked).apply();
        });

        switchInsomnia.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("insomnia", isChecked).apply();
        });

        switchAuditoryAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("auditoryAlerts", isChecked).apply();
        });

        switchVisualAlerts.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("visualAlerts", isChecked).apply();
        });
    }

    private void handleLogout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        // Only clear login session, not account credentials
        editor.remove("isLoggedIn");
        editor.remove("isGuest");
        editor.apply();

        Toast.makeText(ProfileActivity.this, "Logged out successfully", Toast.LENGTH_SHORT).show();

        // Navigate to sign-in page
        Intent intent = new Intent(ProfileActivity.this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
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

}