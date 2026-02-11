package com.example.drowsydrivingdetection;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

public class ProfilePage extends AppCompatActivity {

    EditText name, email, age, experience, drivingHours, otherCondition;
    CheckBox narcolepsy, sleepApnea, insomnia, alerts;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_page);


        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        age = findViewById(R.id.age);


        experience = findViewById(R.id.experience);
        drivingHours = findViewById(R.id.drivingHours);


        narcolepsy = findViewById(R.id.cbNarcolepsy);
        sleepApnea = findViewById(R.id.cbSleepApnea);
        insomnia = findViewById(R.id.cbInsomnia);
        otherCondition = findViewById(R.id.otherCondition);


        alerts = findViewById(R.id.cbAlerts);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        SharedPreferences prefs = getSharedPreferences("UserProfile", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString("name", name.getText().toString());
        editor.putString("email", email.getText().toString());
        editor.putInt("age", Integer.parseInt(age.getText().toString()));
        editor.putInt("experience", Integer.parseInt(experience.getText().toString()));
        editor.putFloat("drivingHours",
                Float.parseFloat(drivingHours.getText().toString()));

        editor.putBoolean("narcolepsy", narcolepsy.isChecked());
        editor.putBoolean("sleepApnea", sleepApnea.isChecked());
        editor.putBoolean("insomnia", insomnia.isChecked());
        editor.putString("otherCondition", otherCondition.getText().toString());

        editor.putBoolean("alertsEnabled", alerts.isChecked());
        editor.putBoolean("profileCompleted", true);

        editor.apply();

        Toast.makeText(this,
                "Profile saved successfully!",
                Toast.LENGTH_SHORT).show();
    }
}
