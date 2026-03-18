package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;

public class SignInActivity extends AppCompatActivity {

    private TextInputEditText username;
    private TextInputEditText password;
    private Button btnContinue;
    private Button btnGuestSignIn;
    private TextView tvCreateAccount;
    private TextView forgotPassword;
    private TextView passBanner;
    private TextView errorBanner;

    private SignInViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        viewModel = new ViewModelProvider(this).get(SignInViewModel.class);

        if (viewModel.isUserLoggedIn()) {
            navigateToHome();
            return;
        }
        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        username = findViewById(R.id.emailIDInput);
        password = findViewById(R.id.passwordInput);
        btnContinue = findViewById(R.id.btnContinue);
        btnGuestSignIn = findViewById(R.id.btnGuestSignIn);
        tvCreateAccount = findViewById(R.id.signUp);
        forgotPassword = findViewById(R.id.forgotPassword);
        passBanner = findViewById(R.id.passBanner);
        errorBanner = findViewById(R.id.errorBanner);
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleEmailSignIn();
            }
        });

        btnGuestSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleGuestSignIn();
            }
        });

        tvCreateAccount.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 navigateToRegistration();
             }
        });

        forgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, ForgotPasswordActivity.class);
                startActivity(intent);
            }
        });
    }

    private void handleEmailSignIn() {
        String email = this.username.getText().toString().trim();
        String pwd = this.password.getText().toString().trim();

        SignInViewModel.SignInResult result = viewModel.handleEmailSignIn(email, pwd);

        if (result.success) {
            showPass(result.message);
            navigateToHome();
        } else if (result.noAccount) {
            showError(result.message);
            navigateToRegistration();
        } else {
            showError(result.message);
        }
    }

    private void handleGuestSignIn() {
        SignInViewModel.SignInResult result = viewModel.handleGuestSignIn();

        if (result.success) {
            showPass(result.message);
            navigateToHome();
        } else {
            showError(result.message);
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

    private void navigateToHome() {
        Intent intent = new Intent(SignInActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToRegistration() {
        Intent intent = new Intent(SignInActivity.this, RegistrationActivity.class);
        startActivity(intent);
    }
}
