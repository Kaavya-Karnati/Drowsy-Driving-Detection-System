package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends NavActivity {

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//    }

//    public void openCamera(View view) {
//        startActivity(new Intent(this, ModelPage.class));
//    }
//
//    public void loadOpenCV(View view) {
//        startActivity(new Intent(this, OpenCV.class));
//    }
//
//    public void openProfile(View view) {
//        startActivity(new Intent(this, ProfilePage.class));
//    }
    private TextView alertCountNumber;
    private TextView auditoryAlertEmail;
    private TextView visualAlertEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false);

        if (!isLoggedIn) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.home_page);

        alertCountNumber = findViewById(R.id.alertCountNumber);
        auditoryAlertEmail = findViewById(R.id.auditoryAlertEmail);
        visualAlertEmail = findViewById(R.id.visualAlertEmail);

        setupBottomNavigation();
        loadHomeData();
    }

    private void loadHomeData() {
        alertCountNumber.setText("45,678 Alerts");
        auditoryAlertEmail.setText("+20 % from last month");
        visualAlertEmail.setText("-5% from last month");
    }


}