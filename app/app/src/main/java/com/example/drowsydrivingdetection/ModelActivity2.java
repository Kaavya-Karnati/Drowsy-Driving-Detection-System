package com.example.drowsydrivingdetection;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.google.android.material.button.MaterialButton;
import android.location.Location;
import android.location.LocationManager;
import android.content.Context;
import android.telephony.SmsManager;
import android.content.pm.PackageManager;

public class ModelActivity2 extends NavActivity {

    private MaterialButton btnStart;
    private MaterialButton btnStop;
    private MaterialButton btnEmergency;
    private View indicatorGreen;
    private View indicatorRed;
    private boolean isDetectionRunning = false;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.model_ui);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnEmergency = findViewById(R.id.btnEmergency);
        indicatorGreen = findViewById(R.id.statusIndicatorGreen);
        indicatorRed = findViewById(R.id.statusIndicatorRed);
        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        indicatorRed.setVisibility(View.VISIBLE);
        indicatorGreen.setVisibility(View.INVISIBLE);

        setupBottomNavigation();
        setupButtonListeners();
    }

    public void openCamera(View view) {
        startActivity(new Intent(this, ModelPage.class));
    }

    public void openCV(View view) {
        startActivity(new Intent(this, OpenCV.class));
    }

    public void openAlert(View view) {
        startActivity(new Intent(this, AlertActivity.class));
    }

    private void setupButtonListeners() {

        btnStart.setOnClickListener(v -> {
            if (!isDetectionRunning) {
                isDetectionRunning = true;
                indicatorRed.setVisibility(View.INVISIBLE);
                indicatorGreen.setVisibility(View.VISIBLE);
                startActivity(new Intent(ModelActivity2.this, ModelPage.class));
                Toast.makeText(ModelActivity2.this, "Drowsy Detection Started", Toast.LENGTH_SHORT).show();
            }
        });

        btnStop.setOnClickListener(v -> {
            if (isDetectionRunning) {
                isDetectionRunning = false;
                indicatorGreen.setVisibility(View.INVISIBLE);
                indicatorRed.setVisibility(View.VISIBLE);
                Toast.makeText(ModelActivity2.this, "Drowsy Detection Stopped", Toast.LENGTH_SHORT).show();
            }
        });

        btnEmergency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendtext();
            }
        });
    }

    private Location lastknownL() {
        LocationManager lmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 2);
            return null;
        }
        Location gps = lmanager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location net = lmanager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        if (gps != null) return gps;
        return net;
    }

    private void sendtext() {
        String email = sharedPreferences.getString("userEmail", null);
        String phone = sharedPreferences.getString("emergencyContact" + email, null);

        if (phone == null || phone.isEmpty()) {
            Toast.makeText(this, "No emergency contact", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 1);
            return;
        }
        try {
            SmsManager text = SmsManager.getDefault();
            Location location = lastknownL();
            String m = "Emergency! I might be drowsy while driving. Please check on me.";
            if (location != null) {
                double l1 = location.getLatitude();
                double l2 = location.getLongitude();
                m += "\nMy last known location: https://maps.google.com/?q=" + l1 + "," + l2;
            }
            else {
                m += "\nLocation unavailable";
            }
            text.sendTextMessage(phone, null, m, null, null);
            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show();
        }
        catch (Exception e) {
            Toast.makeText(this, "Failed to send text", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendtext();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}