package com.example.drowsydrivingdetection;

import android.os.Bundle;

public class FaqActivity extends NavActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.open_cv_activity); //placeholder

        setupBottomNavigation();
    }
}