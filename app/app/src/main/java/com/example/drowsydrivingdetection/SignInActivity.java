package com.example.drowsydrivingdetection;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.textfield.TextInputEditText;

public class SignInActivity extends AppCompatActivity {

    private TextInputEditText username;
    private TextInputEditText password;
    private Button btnContinue;
    private Button btnGuestSignIn;
    private TextView tvCreateAccount;

    private SharedPreferences sharedPreferences;

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
    }

    private void handleEmailSignIn() {
        String username = this.username.getText().toString().trim();
        String password = this.password.getText().toString().trim();

        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate password length
        if (password.length() < 8) {
            Toast.makeText(this, "Password must be at least 8 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        String storedEmail = sharedPreferences.getString("registered_email", null);

        if (storedEmail == null) {
            // No user registered yet
            Toast.makeText(this, "No account found. Please register first.", Toast.LENGTH_LONG).show();
            navigateToRegistration();
        } else if (storedEmail.equals(username)) {
            // User exists
            loginExistingUser(username, password);
        } else {
            // Different email
            Toast.makeText(this, "Email not found. Please check your email or register.", Toast.LENGTH_LONG).show();
        }
    }


    private void loginExistingUser(String email, String password) {
        // Retrieve stored hash
        String storedHash = sharedPreferences.getString("password_hash", null);

        if (storedHash == null) {
            Toast.makeText(this, "Account error. Please register again.", Toast.LENGTH_SHORT).show();
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

            Toast.makeText(this, "Sign in successful!", Toast.LENGTH_SHORT).show();
            navigateToHome();
        } else {
            // Password incorrect
            Toast.makeText(this, "Incorrect password. Please try again.", Toast.LENGTH_SHORT).show();
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

        Toast.makeText(this, "Continuing as guest", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }

    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
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