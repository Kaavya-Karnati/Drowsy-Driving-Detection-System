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


import java.util.regex.Pattern;


public class RegistrationActivity extends AppCompatActivity {


    private TextInputEditText firstName;
    private TextInputEditText lastName;
    private TextInputEditText email;
    private TextInputEditText password;
    private TextInputEditText confirmPassword;
    private TextView backToLogin;
    private Button btnRegister;
    private TextInputEditText securityQuestion1;
    private TextInputEditText securityQuestion2;
    private TextInputEditText securityQuestion3;



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
        securityQuestion1 = findViewById(R.id.securityQuestion1);
        securityQuestion2 = findViewById(R.id.securityQuestion2);
        securityQuestion3 = findViewById(R.id.securityQuestion3);
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
        String securityAnswer1 = this.securityQuestion1.getText().toString().trim();
        String securityAnswer2 = this.securityQuestion2.getText().toString().trim();
        String securityAnswer3 = this.securityQuestion3.getText().toString().trim();


        // Validate inputs
        if (firstName.isEmpty()) {
            Toast.makeText(this, "Please enter your first name", Toast.LENGTH_SHORT).show();
            this.firstName.requestFocus();
            return;
        }


        if (lastName.isEmpty()) {
            Toast.makeText(this, "Please enter your last name", Toast.LENGTH_SHORT).show();
            this.lastName.requestFocus();
            return;
        }


        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show();
            this.email.requestFocus();
            return;
        }


        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            this.email.requestFocus();
            return;
        }


        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show();
            this.password.requestFocus();
            return;
        }


        // Validate password
        if (!isPasswordValid(password)) {
            return;
        }


        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please confirm your password", Toast.LENGTH_SHORT).show();
            this.confirmPassword.requestFocus();
            return;
        }


        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show();
            this.confirmPassword.requestFocus();
            return;
        }


        if (securityAnswer1.isEmpty()) {
            Toast.makeText(this, "Please answer Security Question 1", Toast.LENGTH_SHORT).show();
            this.securityQuestion1.requestFocus();
            return;
        }


        if (securityAnswer2.isEmpty()) {
            Toast.makeText(this, "Please answer Security Question 2", Toast.LENGTH_SHORT).show();
            this.securityQuestion2.requestFocus();
            return;
        }


        if (securityAnswer3.isEmpty()) {
            Toast.makeText(this, "Please answer Security Question 3", Toast.LENGTH_SHORT).show();
            this.securityQuestion3.requestFocus();
            return;
        }


        // Check if email is already registered
        String existingEmail = sharedPreferences.getString("registered_email", null);
        if (existingEmail != null && existingEmail.equals(email)) {
            Toast.makeText(this, "Email already registered. Please sign in instead.", Toast.LENGTH_LONG).show();
            navigateToSignIn();
            return;
        }

        registerUser(firstName, lastName, email, password, securityAnswer1, securityAnswer2, securityAnswer3);
    }


    private void registerUser(String firstName, String lastName, String email, String password, String securityAnswer1, String securityAnswer2, String securityAnswer3) {
        // Hash the password with BCrypt
        String hashedPassword = SecurityUtils.hashPassword(password);


        if (hashedPassword == null) {
            Toast.makeText(this, "Error creating account. Please try again.", Toast.LENGTH_SHORT).show();
            return;
        }


        // Save user data
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("registered_email", email);
        editor.putString("password_hash", hashedPassword);
        editor.putString("userFirstName", firstName);
        editor.putString("userLastName", lastName);
        editor.putString("userName", firstName + " " + lastName);
        editor.putString("userEmail", email);
        editor.putBoolean("isLoggedIn", true);
        editor.putBoolean("isGuest", false);
        editor.putString("securityAnswer1", securityAnswer1);
        editor.putString("securityAnswer2", securityAnswer2);
        editor.putString("securityAnswer3", securityAnswer3);
        editor.apply();


        Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();
        navigateToHome();
    }


    private boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }


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
            Toast.makeText(this, "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long",
                    Toast.LENGTH_SHORT).show();
            this.password.requestFocus();
            return false;
        }


        // Check for at least one digit
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            Toast.makeText(this, "Password must contain at least one digit (0-9)",
                    Toast.LENGTH_SHORT).show();
            this.password.requestFocus();
            return false;
        }


        // Check for at least one special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            Toast.makeText(this, "Password must contain at least one special character (!@#$%^&*)",
                    Toast.LENGTH_SHORT).show();
            this.password.requestFocus();
            return false;
        }

        return true;
    }
}
