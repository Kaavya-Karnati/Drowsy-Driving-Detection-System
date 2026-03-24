package com.example.drowsydrivingdetection.ui.pages;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.viewmodel.ResetPasswordViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class ResetPasswordActivity extends AppCompatActivity {

    private TextInputEditText newPassword;
    private TextInputEditText confirmPassword;
    private Button btnReset;
    private TextView passBanner;
    private TextView errorBanner;

    private ResetPasswordViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        viewModel = new ViewModelProvider(this).get(ResetPasswordViewModel.class);

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        newPassword = findViewById(R.id.newPassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnReset = findViewById(R.id.btnReset);
        passBanner = findViewById(R.id.passBanner);
        errorBanner = findViewById(R.id.errorBanner);
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

        ResetPasswordViewModel.ResetResult result = viewModel.handleResetPassword(pass1, pass2);

        if (result.success) {
            showPass(result.message);
            navigateToSignIn();
        } else {
            if (result.isPasswordPolicyError) {
                newPassword.setError(result.message);
                newPassword.requestFocus();
            } else if (result.focusField != null) {
                applyFieldFocus(result.focusField);
                showError(result.message);
            } else {
                showError(result.message);
            }
        }
    }

    private void applyFieldFocus(ResetPasswordViewModel.FocusField field) {
        switch (field) {
            case NEW_PASSWORD:
                newPassword.requestFocus();
                break;
            case CONFIRM_PASSWORD:
                confirmPassword.requestFocus();
                break;
        }
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

    private void navigateToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
