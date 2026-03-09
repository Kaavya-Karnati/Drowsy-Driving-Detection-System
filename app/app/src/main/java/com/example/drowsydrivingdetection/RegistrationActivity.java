package com.example.drowsydrivingdetection;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

import java.util.regex.Pattern;

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

    private SharedPreferences sharedPreferences;

    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*].*");
    private static final int MIN_PASSWORD_LENGTH = 8;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.registration_activity);

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
        String confirmPassword = this.confirmPassword.getText().toString().trim();
        // Ahmed's code
        String securityAnswer1 = this.securityQuestion1.getText().toString().trim();
        String securityAnswer2 = this.securityQuestion2.getText().toString().trim();
        String securityAnswer3 = this.securityQuestion3.getText().toString().trim();
        //end

        // Validate inputs
        if (firstName.isEmpty()) {
            //Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
            showError("Please enter your first name");
            this.firstName.requestFocus();
            return;
        }

        if (lastName.isEmpty()) {
            //Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show();
            showError("Please enter your last name");
            this.lastName.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            //Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            showError("Please enter your email");
            this.email.requestFocus();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            //Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            showError("Please enter a valid email address");
            this.email.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            //Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            showError("Please enter a password");
            this.password.requestFocus();
            return;
        }

        // Validate password
        if (!isPasswordValid(password)) {
            return;
        }

        if (confirmPassword.isEmpty()) {
            //Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            showError("Please confirm your password");
            this.confirmPassword.requestFocus();
            return;
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            //Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            showError("Passwords do not match");
            this.confirmPassword.requestFocus();
            return;
        }

        // Ahmed's Code
        if (securityAnswer1.isEmpty()) {
            showError("Please answer Security Question 1");
            this.securityQuestion1.requestFocus();
            return;
        }


        if (securityAnswer2.isEmpty()) {
            showError("Please answer Security Question 2");
            this.securityQuestion2.requestFocus();
            return;
        }


        if (securityAnswer3.isEmpty()) {
            showError("Please answer Security Question 3");
            this.securityQuestion3.requestFocus();
            return;
        }
        // end

        // Check if email is already registered
        String existingEmail = sharedPreferences.getString("registered_email", null);
        if (existingEmail != null && existingEmail.equals(email)) {
            //Toast.makeText(this, "Email already registered. Please sign in instead.", Toast.LENGTH_LONG).show();
            showErrorForTransition("Email already registered. Please sign in instead.");
            navigateToSignIn();
            return;
        }

        registerUser(firstName, lastName, email, password, securityAnswer1, securityAnswer2, securityAnswer3);
    }

    private void registerUser(String firstName, String lastName, String email, String password, String securityAnswer1, String securityAnswer2, String securityAnswer3) {
        // Hash the password with BCrypt
        String hashedPassword = SecurityUtils.hashPassword(password);

        if (hashedPassword == null) {
            //Toast.makeText(this, "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
            showError("Error creating account. Please try again.");
            return;
        }

        // Save user data
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("registered_email", email);
        editor.putString("password_hash", hashedPassword);

        editor.putString("userFirstName_" + email, firstName);
        editor.putString("userLastName_" + email, lastName);
        editor.putString("userName_" + email, firstName + " " + lastName);
        editor.putString("userEmail", email);
        editor.putBoolean("isLoggedIn", true);
        editor.putBoolean("isGuest", false);
        editor.putString("securityAnswer1", securityAnswer1);
        editor.putString("securityAnswer2", securityAnswer2);
        editor.putString("securityAnswer3", securityAnswer3);
        editor.apply();

        //Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        showPass("Account created successfully!");
        navigateToHome();
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
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

    private boolean isPasswordValid(String password) {
        // Check minimum length
        if (password.length() < MIN_PASSWORD_LENGTH) {
            //Toast.makeText(this, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long", Toast.LENGTH_SHORT).show();
            this.password.setError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");            
            this.password.requestFocus();
            return false;
        }

        // Check for at least one digit
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            //Toast.makeText(this, "Password must contain at least one digit (0-9)", Toast.LENGTH_SHORT).show();
            this.password.setError("Password must contain at least one digit (0-9)");
            this.password.requestFocus();
            return false;
        }

        // Check for at least one special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            //Toast.makeText(this, "Password must contain at least one special character (!@#$%^&*)", Toast.LENGTH_SHORT).show();
            this.password.setError("Password must contain at least one special character (!@#$%^&*)");
            this.password.requestFocus();
            return false;
        }

        return true;
    }

}
