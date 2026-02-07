package com.example.drowsydrivingdetection;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class RegistrationActivity extends AppCompatActivity {

    EditText username, password;
    Button register;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        username = findViewById(R.id.Rusername);
        password = findViewById(R.id.Rpassword);
        register = findViewById(R.id.Register);

        register.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("info_stuff", MODE_PRIVATE);

            String user = username.getText().toString().trim();
            String pass = password.getText().toString().trim();

            if (user.isEmpty()) {
                username.setError("Required");
                return;
            }

            if (pass.isEmpty()) {
                password.setError("Required");
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", user);
            editor.putString("password", pass);
            editor.apply();

            Toast.makeText(this, "Registered successfully", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
