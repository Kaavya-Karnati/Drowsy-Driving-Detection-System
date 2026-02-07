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
    CheckBox cbNarcolepsy, cbSleepApnea, cbInsomnia, cbAlerts;
    Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile_page);

        // BASIC INFO
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        age = findViewById(R.id.age);

        // DRIVING INFO
        experience = findViewById(R.id.experience);
        drivingHours = findViewById(R.id.drivingHours);

        // HEALTH INFO
        cbNarcolepsy = findViewById(R.id.cbNarcolepsy);
        cbSleepApnea = findViewById(R.id.cbSleepApnea);
        cbInsomnia = findViewById(R.id.cbInsomnia);
        otherCondition = findViewById(R.id.otherCondition);

        // SETTINGS
        cbAlerts = findViewById(R.id.cbAlerts);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void clearFields() {
        name.setText("");
        email.setText("");
        age.setText("");
        experience.setText("");
        drivingHours.setText("");
        otherCondition.setText("");

        cbNarcolepsy.setChecked(false);
        cbSleepApnea.setChecked(false);
        cbInsomnia.setChecked(false);
        cbAlerts.setChecked(false);
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

        editor.putBoolean("narcolepsy", cbNarcolepsy.isChecked());
        editor.putBoolean("sleepApnea", cbSleepApnea.isChecked());
        editor.putBoolean("insomnia", cbInsomnia.isChecked());
        editor.putString("otherCondition", otherCondition.getText().toString());

        editor.putBoolean("alertsEnabled", cbAlerts.isChecked());
        editor.putBoolean("profileCompleted", true);

        editor.apply();

        Toast.makeText(this,
                "Profile saved successfully!",
                Toast.LENGTH_SHORT).show();

        clearFields();
    }
}
