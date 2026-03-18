package com.example.drowsydrivingdetection.ui.pages;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.drowsydrivingdetection.MainActivity;
import com.example.drowsydrivingdetection.R;
import com.example.drowsydrivingdetection.viewmodel.RegistrationViewModel;
import com.google.android.material.textfield.TextInputEditText;

public class RegistrationActivity extends AppCompatActivity {

    private TextInputEditText firstName;
    private TextInputEditText lastName;
    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText confirmPassword;
    private TextView backToLogin;
    private Button btnRegister;
    // Ahmed's code
    private TextInputEditText securityQuestion1;
    private TextInputEditText securityQuestion2;
    private TextInputEditText securityQuestion3;
    private TextView passBanner;
    private TextView errorBanner;
    // end

    private RegistrationViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);

        viewModel = new ViewModelProvider(this).get(RegistrationViewModel.class);

        // Check if user is already logged in
        if (viewModel.isUserLoggedIn()) {
            navigateToHome();
            return;
        }

        initializeViews();
        setupListeners();
    }

    private void initializeViews() {
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        email = findViewById(R.id.Rusername);
        password = findViewById(R.id.Rpassword);
        confirmPassword = findViewById(R.id.confirmPassword);
        btnRegister = findViewById(R.id.Register);
        backToLogin = findViewById(R.id.backToLogIn);
        // Ahmed's code
        securityQuestion1 = findViewById(R.id.securityQuestion1);
        securityQuestion2 = findViewById(R.id.securityQuestion2);
        securityQuestion3 = findViewById(R.id.securityQuestion3);
        passBanner = findViewById(R.id.passBanner);
        errorBanner = findViewById(R.id.errorBanner);
        // end
    }

    private void setupListeners() {
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegistration();
            }
        });

        backToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToSignIn();
            }
        });
    }

    private void handleRegistration() {
        // Get input values
        String firstName = this.firstName.getText().toString().trim();
        String lastName = this.lastName.getText().toString().trim();
        String email = this.email.getText().toString().trim();
        String password = this.password.getText().toString().trim();

        // Ahmed's code
        String confirmPassword = this.confirmPassword.getText().toString().trim();
        String securityAnswer1 = this.securityQuestion1.getText().toString().trim();
        String securityAnswer2 = this.securityQuestion2.getText().toString().trim();
        String securityAnswer3 = this.securityQuestion3.getText().toString().trim();
        //end

        RegistrationViewModel.RegistrationResult result = viewModel.handleRegistration(
                firstName,
                lastName,
                email,
                password,
                confirmPassword,
                securityAnswer1,
                securityAnswer2,
                securityAnswer3
        );

        if (result == null) {
            return;
        }

        if (result.success) {
            showPass(result.message);
            navigateToHome();
        } else {
            if (result.emailAlreadyRegistered) {
                showErrorForTransition(result.message);
                navigateToSignIn();
            } else {
                applyFieldFocusForError(result.message);
                showError(result.message);
            }
        }
    }

    private void applyFieldFocusForError(String message) {
        if (message == null) {
            return;
        }

        if (message.contains("first name")) {
            this.firstName.requestFocus();
        } else if (message.contains("last name")) {
            this.lastName.requestFocus();
        } else if (message.contains("email")) {
            this.email.requestFocus();
        } else if (message.contains("Password must be at least")) {
            this.password.setError(message);
            this.password.requestFocus();
        } else if (message.contains("digit (0-9)")) {
            this.password.setError(message);
            this.password.requestFocus();
        } else if (message.contains("special character")) {
            this.password.setError(message);
            this.password.requestFocus();
        } else if (message.contains("confirm your password")) {
            this.confirmPassword.requestFocus();
        } else if (message.contains("Passwords do not match")) {
            this.confirmPassword.requestFocus();
        } else if (message.contains("Security Question 1")) {
            this.securityQuestion1.requestFocus();
        } else if (message.contains("Security Question 2")) {
            this.securityQuestion2.requestFocus();
        } else if (message.contains("Security Question 3")) {
            this.securityQuestion3.requestFocus();
        }
    }

    // Ahmed's Code
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

    private void showErrorForTransition(String message) {
        errorBanner.setText(message);
        errorBanner.setVisibility(View.VISIBLE);
        errorBanner.setAlpha(1f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                errorBanner.animate()
                        .alpha(0f)
                        .setDuration(500)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                errorBanner.setVisibility(View.GONE);
                            }
                        });
            }
        }, 100000);
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
    //end of Ahmed's Code

    private void navigateToHome() {
        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToSignIn() {
        Intent intent = new Intent(RegistrationActivity.this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

}
