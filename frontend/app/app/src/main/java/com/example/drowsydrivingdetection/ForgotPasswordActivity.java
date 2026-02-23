package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText firstName;
    private TextInputEditText lastName;
    private TextInputEditText email;
    private TextInputEditText answer1;
    private TextInputEditText answer2;
    private TextInputEditText answer3;
    private Button btnVerify;
    private TextView backToLogin;

    private SharedPreferences sharedPreferences;

    private static final int max = 3;
    private static final long lock = 60 * 60 * 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        firstName = findViewById(R.id.fpFirstName);
        lastName = findViewById(R.id.fpLastName);
        email = findViewById(R.id.fpEmail);
        answer1 = findViewById(R.id.fpAnswer1);
        answer2 = findViewById(R.id.fpAnswer2);
        answer3 = findViewById(R.id.fpAnswer3);
        btnVerify = findViewById(R.id.btnVerify);
        backToLogin = findViewById(R.id.backToLogin);
    }

    private void setupListeners() {
        btnVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleVerification();
            }
        });

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void handleVerification() {
        String fFirstName = firstName.getText().toString().trim();
        String fLastName = lastName.getText().toString().trim();
        String fEmail = email.getText().toString().trim().toLowerCase();
        String a1 = answer1.getText().toString().trim();
        String a2 = answer2.getText().toString().trim();
        String a3 = answer3.getText().toString().trim();

        if (fFirstName.isEmpty()) {
            Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
            firstName.requestFocus();
            return;
        }

        if (fLastName.isEmpty()) {
            Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show();
            lastName.requestFocus();
            return;
        }

        if (fEmail.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(fEmail).matches()) {
            Toast.makeText(this, "Enter email address in the right format", Toast.LENGTH_SHORT).show();
            email.requestFocus();
            return;
        }

        if (a1.isEmpty()) {
            Toast.makeText(this, "Please answer Security Question 1", Toast.LENGTH_SHORT).show();
            answer1.requestFocus();
            return;
        }

        if (a2.isEmpty()) {
            Toast.makeText(this, "Please answer Security Question 2", Toast.LENGTH_SHORT).show();
            answer2.requestFocus();
            return;
        }

        if (a3.isEmpty()) {
            Toast.makeText(this, "Please answer Security Question 3", Toast.LENGTH_SHORT).show();
            answer3.requestFocus();
            return;
        }

        String sFirstName = sharedPreferences.getString("userFirstName", null);
        String sLastName = sharedPreferences.getString("userLastName", null);
        String sEmail = sharedPreferences.getString("registered_email", null);

        if (sFirstName == null || sLastName == null || sEmail == null) {
            Toast.makeText(this, "Account doesn't exist", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!sEmail.equalsIgnoreCase(fEmail)) {
            Toast.makeText(this, "Invalid email", Toast.LENGTH_SHORT).show();
            email.requestFocus();
            return;
        }

        String attemptKey = "resetattempts" + fEmail;
        String lockKey = "resetlocktime" + fEmail;

        long lockTime = sharedPreferences.getLong(lockKey, 0);
        if (System.currentTimeMillis() < lockTime) {
            long remainingMillis = lockTime - System.currentTimeMillis();
            long remainingMinutes = (remainingMillis / 60000) + 1;
            Toast.makeText(this, "Too many attempts. Try again in " + remainingMinutes + " minutes.", Toast.LENGTH_LONG).show();
            goToLogin();
            return;
        }

        if (!sFirstName.equalsIgnoreCase(fFirstName)) {
            Toast.makeText(this, "Invalid details", Toast.LENGTH_SHORT).show();
            firstName.requestFocus();
            return;
        }

        if (!sLastName.equalsIgnoreCase(fLastName)) {
            Toast.makeText(this, "Invalid details", Toast.LENGTH_SHORT).show();
            lastName.requestFocus();
            return;
        }

        String s1 = sharedPreferences.getString("securityAnswer1", "").trim();
        String s2 = sharedPreferences.getString("securityAnswer2", "").trim();
        String s3 = sharedPreferences.getString("securityAnswer3", "").trim();

        int correct = 0;
        if (a1.equalsIgnoreCase(s1)) correct++;
        if (a2.equalsIgnoreCase(s2)) correct++;
        if (a3.equalsIgnoreCase(s3)) correct++;

        if (correct >= 2) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(attemptKey, 0);
            editor.putLong(lockKey, 0);
            editor.apply();
            Toast.makeText(this, "Passed!", Toast.LENGTH_SHORT).show();
            navigateToResetPassword();
        } else {
            attempt(fEmail);
        }
    }

    private void attempt(String email) {
        String attemptKey = "resetattempts" + email;
        String lockKey = "resetlocktime" + email;

        int attempts = sharedPreferences.getInt(attemptKey, 0);
        attempts++;

        if (attempts >= max) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(lockKey, System.currentTimeMillis() + lock);
            editor.putInt(attemptKey, 0);
            editor.apply();
            Toast.makeText(this, "Too many failed attempts. Locked for 1 hour.", Toast.LENGTH_LONG).show();
            goToLogin();
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(attemptKey, attempts);
            editor.apply();
            int remaining = max - attempts;
            Toast.makeText(this, "Incorrect answers. " + remaining + " attempts remaining.", Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateToResetPassword() {
        Intent intent = new Intent(ForgotPasswordActivity.this, ResetPasswordActivity.class);
        startActivity(intent);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(ForgotPasswordActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }
}