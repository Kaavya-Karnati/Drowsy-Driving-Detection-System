package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Immediately open Profile Page
        Intent intent = new Intent(this, ProfilePage.class);
        startActivity(intent);

        // Close MainActivity so user can't go back to it
        finish();
    }
}