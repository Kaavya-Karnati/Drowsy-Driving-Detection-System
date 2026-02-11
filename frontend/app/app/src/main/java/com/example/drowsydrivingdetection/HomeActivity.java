package com.example.drowsydrivingdetection;


import android.os.Bundle;
import android.widget.TextView;

public class HomeActivity extends NavActivity {

    private TextView alertCountNumber;
    private TextView auditoryAlertEmail;
    private TextView visualAlertEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
