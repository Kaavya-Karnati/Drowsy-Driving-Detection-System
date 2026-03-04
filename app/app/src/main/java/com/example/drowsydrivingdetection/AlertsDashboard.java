package com.example.drowsydrivingdetection;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class AlertsDashboard extends NavActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_records);

        setupBottomNavigation();
    }


}
