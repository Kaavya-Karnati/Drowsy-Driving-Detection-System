package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openCamera(View view) {
        startActivity(new Intent(this, ModelPage.class));
    }

    public void openProfile(View view) {
        startActivity(new Intent(this, ProfilePage.class));
    }

}