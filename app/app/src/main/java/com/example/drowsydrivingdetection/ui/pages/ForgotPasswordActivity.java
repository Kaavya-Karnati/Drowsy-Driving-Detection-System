package com.example.drowsydrivingdetection.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.viewmodel.ForgotPasswordViewModel;
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

    private ForgotPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        viewModel = new ViewModelProvider(this).get(ForgotPasswordViewModel.class);

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

        ForgotPasswordViewModel.VerificationResult result =
                viewModel.handleVerification(fFirstName, fLastName, fEmail, a1, a2, a3);

        if (result.success) {
            showPass(result.message);
            navigateToResetPassword();
        } else if (result.lockedOut) {
            showError(result.message);
            goToLogin();
        } else {
            if (result.focusField != null) {
                applyFieldFocus(result.focusField);
            }
            showError(result.message);
        }
    }

    private void applyFieldFocus(ForgotPasswordViewModel.FocusField field) {
        switch (field) {
            case FIRST_NAME:
                firstName.requestFocus();
                break;
            case LAST_NAME:
                lastName.requestFocus();
                break;
            case EMAIL:
                email.requestFocus();
                break;
            case ANSWER1:
                answer1.requestFocus();
                break;
            case ANSWER2:
                answer2.requestFocus();
                break;
            case ANSWER3:
                answer3.requestFocus();
                break;
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
