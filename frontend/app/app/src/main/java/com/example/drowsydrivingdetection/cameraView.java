package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.opencv.android.OpenCVLoader;

public class cameraView extends AppCompatActivity {

    // Anthony
    Button returnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_page);

        if (OpenCVLoader.initLocal()) {
            Log.d("LOADED", "OpenCV successfully loaded.");
        } else {
            Log.e("LOADED", "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        returnToScreen();
    }

    // Return button functionality
    private void returnToScreen(){
        returnButton = findViewById(R.id.returnButton);

        returnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                finish();

            }
        });
    }

}
