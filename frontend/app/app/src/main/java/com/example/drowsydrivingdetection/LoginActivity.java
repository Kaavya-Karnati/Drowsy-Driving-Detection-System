package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class LoginActivity extends AppCompatActivity {

    TextInputEditText emailInput, passwordInput;
    MaterialButton btnContinue, btnGuestSignIn, btnGoogleSignIn;
    TextView signUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Connect to new XML IDs
        emailInput = findViewById(R.id.emailIDHint);
        passwordInput = findViewById(R.id.passwordInput);
        btnContinue = findViewById(R.id.btnContinue);
        btnGuestSignIn = findViewById(R.id.btnGuestSignIn);
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn);
        signUp = findViewById(R.id.signUp);

        // LOGIN BUTTON (Continue)
        btnContinue.setOnClickListener(v -> {

            SharedPreferences prefs = getSharedPreferences("info_stuff", MODE_PRIVATE);

            String savedU = prefs.getString("username", null);
            String savedP = prefs.getString("password", null);

            String inputU = emailInput.getText().toString().trim();
            String inputP = passwordInput.getText().toString().trim();

            if (savedU == null || savedP == null) {
                Toast.makeText(this, "Account does not exist", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputU.equals(savedU) && inputP.equals(savedP)) {
                Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ProfilePage.class));
                finish();
            } else {
                Toast.makeText(this, "Wrong email or password", Toast.LENGTH_SHORT).show();
            }
        });

        //
        signUp.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class))
        );

        // Guest login
        btnGuestSignIn.setOnClickListener(v -> {
            Toast.makeText(this, "Continuing as Guest", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, ProfilePage.class));
        });

        // Google sign in (placeholder for now)
        btnGoogleSignIn.setOnClickListener(v -> {
            Toast.makeText(this, "Google Sign-In not implemented yet", Toast.LENGTH_SHORT).show();
        });
    }
}
