package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;


public class SignInActivity extends AppCompatActivity {


    private TextInputEditText username;
    private TextInputEditText password;
    private Button btnContinue;
    private Button btnGuestSignIn;
    private TextView tvCreateAccount;
    private TextView forgotPassword;
    private TextView passBanner;
    private TextView errorBanner;


    private SharedPreferences sharedPreferences;

    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*].*");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);


        sharedPreferences = getSharedPreferences("DrowsyDriverPrefs", MODE_PRIVATE);


        // Check if user is already logged in
        if (isUserLoggedIn()) {
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
        String username = this.username.getText().toString().trim();
        String password = this.password.getText().toString().trim();


        if (username.isEmpty()) {
            showError("Please enter your email");
            return;
        }


        if (password.isEmpty()) {
            showError("Please enter your password");
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            showError("Please enter a valid email address");
            return;
        }

        // Validate password length
        if (password.length() < 8) {
            showError("Invalid Password");
            return;
        }

        if (!DIGIT_PATTERN.matcher(password).matches()) {
            showError("Invalid Password");
            return;
        }


        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            showError("Invalid Password");
            return;
        }


        String storedEmail = sharedPreferences.getString("registered_email", null);


        if (storedEmail == null) {
            // No user registered yet
            showError("No account found. Please register first.");
            navigateToRegistration();
        } else if (storedEmail.equals(username)) {
            // User exists
            loginExistingUser(username, password);
        } else {
            // Different email
            showError("Email not found. Please check your email or register.");
        }
    }


    private void loginExistingUser(String email, String password) {
        // Retrieve stored hash
        String storedHash = sharedPreferences.getString("password_hash", null);


        if (storedHash == null) {
            showError("Account error. Please register again.");
            return;
        }

        // Verify password with BCrypt
        if (SecurityUtils.verifyPassword(password, storedHash)) {
            // Password correct - log in
            String firstName = sharedPreferences.getString("userFirstName", "User");
            String lastName = sharedPreferences.getString("userLastName", "");
            String fullName = (firstName + " " + lastName).trim();


            if (fullName.isEmpty()) {
                fullName = "User";
            }


            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putBoolean("isGuest", false);
            editor.putString("userEmail", email);
            editor.putString("userName", fullName);
            editor.apply();


            showPass("Sign in successful!");
            navigateToHome();
        } else {
            // Password incorrect
            showError("Incorrect password. Please try again.");
        }
    }


    private void handleGuestSignIn() {
        // Save guest mode
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putBoolean("isGuest", true);
        editor.putString("userEmail", "Guest");
        editor.putString("userName", "Guest User");
        editor.putString("userFirstName", "Guest");
        editor.putString("userLastName", "User");
        editor.apply();


        showPass("Continuing as guest");
        navigateToHome();
    }


    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
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
