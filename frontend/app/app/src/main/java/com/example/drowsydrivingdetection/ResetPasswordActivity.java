package com.example.drowsydrivingdetection;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText newPassword;
    private TextInputEditText confirmPassword;
    private Button btnReset;
    private TextView passBanner;

    private SharedPreferences sharedPreferences;


    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*].*");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        newPassword = findViewById(R.id.newPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnReset = findViewById(R.id.btnReset);
        passBanner = findViewById(R.id.passBanner);
    }

    private void setupListeners() {
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleResetPassword();
            }
        });
    }

    private void handleResetPassword() {

        String pass1 = newPassword.getText().toString().trim();
        String pass2 = confirmPassword.getText().toString().trim();


        if (pass1.isEmpty()) {
            Toast.makeText(this, "Please enter a new password", Toast.LENGTH_SHORT).show();
            newPassword.requestFocus();
            return;
        }

        if (pass2.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            confirmPassword.requestFocus();
            return;
        }

        if (!pass1.equals(pass2)) {
            confirmPassword.setError("Passwords do not match");
            confirmPassword.requestFocus();
            return;
        }

        if (pass1.length() < 8) {
            newPassword.setError("Password must be at least 8 characters");
            newPassword.requestFocus();
            return;
        }


        if (!DIGIT_PATTERN.matcher(pass1).matches()) {
            Toast.makeText(this, "Password must contain at least one digit (0-9)", Toast.LENGTH_SHORT).show();
            newPassword.requestFocus();
            return;
        }

        if (!SPECIAL_CHAR_PATTERN.matcher(pass1).matches()) {
            Toast.makeText(this, "Password must contain at least one special character (!@#$%^&*)", Toast.LENGTH_SHORT).show();
            newPassword.requestFocus();
            return;
        }


        String hashedPassword = SecurityUtils.hashPassword(pass1);
        if (hashedPassword == null) {
            Toast.makeText(this, "Error. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("password_hash", hashedPassword);
        editor.apply();

        showPass("Password Was Changed!");

        navigateToSignIn();
    }

    private void showPass(String message) {
        passBanner.setText(message);
        passBanner.setVisibility(View.VISIBLE);
        passBanner.setAlpha(1f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                passBanner.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                passBanner.setVisibility(View.GONE);
                                navigateToSignIn();
                            }
                        });
            }
        }, 100000);
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}