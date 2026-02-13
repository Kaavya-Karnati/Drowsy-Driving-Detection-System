package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class ProfileNonRegisteredActivity extends NavActivity {

    private Button btnSaveContactInfo;
    private Button btnChangeAlertPreferences;
    private Button btnMonitorData;
    private Button btnRegisterHere;

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
    }

    private void setupListeners() {
        btnSaveContactInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileNonRegisteredActivity.this,
                        "Please register to save contact info", Toast.LENGTH_SHORT).show();
            }
        });

        btnChangeAlertPreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileNonRegisteredActivity.this,
                        "Please register to change alert preferences", Toast.LENGTH_SHORT).show();
            }
        });

        btnMonitorData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(ProfileNonRegisteredActivity.this,
                        "Please register to monitor your data", Toast.LENGTH_SHORT).show();
            }
        });

        btnRegisterHere.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToRegistration();
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