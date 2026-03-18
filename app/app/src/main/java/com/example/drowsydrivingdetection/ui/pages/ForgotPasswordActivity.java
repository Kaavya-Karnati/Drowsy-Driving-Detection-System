package com.example.drowsydrivingdetection.ui.pages;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Patterns;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.drowsydrivingdetection.R;
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
    private TextView errorBanner;
    private TextView passBanner;

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
        errorBanner = findViewById(R.id.errorBanner);
        passBanner = findViewById(R.id.passBanner);
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
            showError("Please enter your first name");
            firstName.requestFocus();
            return;
        }

        if (fLastName.isEmpty()) {
            showError("Please enter your last name");
            lastName.requestFocus();
            return;
        }

        if (fEmail.isEmpty()) {
            showError("Please enter your email");
            email.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(fEmail).matches()) {
            showError("Enter email address in the right format");
            email.requestFocus();
            return;
        }

        if (a1.isEmpty()) {
            showError("Please answer Security Question 1");
            answer1.requestFocus();
            return;
        }

        if (a2.isEmpty()) {
            showError("Please answer Security Question 2");
            answer2.requestFocus();
            return;
        }

        if (a3.isEmpty()) {
            showError("Please answer Security Question 3");
            answer3.requestFocus();
            return;
        }

        String sEmail = sharedPreferences.getString("registered_email", null);
        String sFirstName = sharedPreferences.getString("userFirstName_" + sEmail, null);
        String sLastName = sharedPreferences.getString("userLastName_" + sEmail, null);


        if (sFirstName == null || sLastName == null || sEmail == null) {
            showError("Account doesn't exist");
            return;
        }

        if (!sEmail.equalsIgnoreCase(fEmail)) {
            showError("Invalid details");
            email.requestFocus();
            return;
        }

        String attemptKey = "resetattempts" + fEmail;
        String lockKey = "resetlocktime" + fEmail;

        long lockTime = sharedPreferences.getLong(lockKey, 0);
        if (System.currentTimeMillis() < lockTime) {
            long remainingMillis = lockTime - System.currentTimeMillis();
            long remainingMinutes = (remainingMillis / 60000) + 1;
            showError("Too many attempts. Try again in " + remainingMinutes + " minutes.");
            goToLogin();
            return;
        }

        if (!sFirstName.equalsIgnoreCase(fFirstName)) {
            showError("Invalid details");
            firstName.requestFocus();
            return;
        }

        if (!sLastName.equalsIgnoreCase(fLastName)) {
            showError("Invalid details");
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
            showPass("Passed!");
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
            showError("Too many failed attempts. Locked for 1 hour.");
            goToLogin();
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(attemptKey, attempts);
            editor.apply();
            int remaining = max - attempts;
            showError("Wrong answer! " + remaining + " attempts remaining.");
        }
    }

    private void showError(String message) {
        errorBanner.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
        errorBanner.setAlpha(0f);
        errorBanner.animate()
                .alpha(1f)
                .setDuration(300)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        errorBanner.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                errorBanner.animate()
                                        .alpha(0f)
                                        .setDuration(300)
                                        .withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                errorBanner.setVisibility(View.GONE);
                                            }
                                        });
                            }
                        }, 2000);
                    }
                });
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
                            }
                        });
            }
        }, 100000);
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