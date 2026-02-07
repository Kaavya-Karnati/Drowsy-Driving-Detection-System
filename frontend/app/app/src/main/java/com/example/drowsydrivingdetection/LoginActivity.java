package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText username, password;
    Button loginBtn, registerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        username = findViewById(R.id.username);
        password = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginBtn);
        registerBtn = findViewById(R.id.registerBtn);

        loginBtn.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("info_stuff", MODE_PRIVATE);

            String savedU = prefs.getString("username", null);
            String savedP = prefs.getString("password", null);

            String inputU = username.getText().toString().trim();
            String inputP = password.getText().toString().trim();

            if (savedU == null || savedP == null) {
                Toast.makeText(this, "Account does not exist", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputU.equals(savedU) && inputP.equals(savedP)) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ProfilePage.class));
                finish();
            } else {
                Toast.makeText(this, "Wrong username or password", Toast.LENGTH_SHORT).show();
            }
        });

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class))
        );
    }
}
