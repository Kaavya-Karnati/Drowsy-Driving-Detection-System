package com.example.drowsydrivingdetection.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.drowsydrivingdetection.security.PasswordValidator;
import com.example.drowsydrivingdetection.security.SecurityUtils;

public class RegistrationViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPreferences;

    public RegistrationViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("DrowsyDriverPrefs", Application.MODE_PRIVATE);
    }

    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    public RegistrationResult handleRegistration(
            String firstName,
            String lastName,
            String email,
            String password,
            String confirmPassword,
            String securityAnswer1,
            String securityAnswer2,
            String securityAnswer3,
            String emergencyNumber
    ) {
        //validate inputs
        if (isNullOrEmpty(firstName)) {
            return RegistrationResult.error("Please enter your first name");
        }

        if (isNullOrEmpty(lastName)) {
            return RegistrationResult.error("Please enter your last name");
        }

        if (isNullOrEmpty(email)) {
            return RegistrationResult.error("Please enter your email");
        }

        // Validate email format
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return RegistrationResult.error("Please enter a valid email address");
        }

        if (isNullOrEmpty(password)) {
            return RegistrationResult.error("Please enter a password");
        }

        PasswordValidator.Result passwordResult = PasswordValidator.validate(password);
        if (!passwordResult.isValid) {
            return RegistrationResult.error(passwordResult.message);
        }

        if (isNullOrEmpty(confirmPassword)) {
            return RegistrationResult.error("Please confirm your password");
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            return RegistrationResult.error("Passwords do not match");
        }

        // Ahmed's Code
        if (isNullOrEmpty(securityAnswer1)) {
            return RegistrationResult.error("Please answer Security Question 1");
        }

        if (isNullOrEmpty(securityAnswer2)) {
            return RegistrationResult.error("Please answer Security Question 2");
        }

        if (isNullOrEmpty(securityAnswer3)) {
            return RegistrationResult.error("Please answer Security Question 3");
        }
        // end
        //Ahmed: check if user entered an emergency contact number
        if (isNullOrEmpty(emergencyNumber)) {
            return RegistrationResult.error("Please enter an emergency contact number");
        }

        //check if email is already registered
        String existingEmail = sharedPreferences.getString("registered_email", null);
        if (existingEmail != null && existingEmail.equals(email)) {
            return RegistrationResult.emailAlreadyRegistered("Email already registered. Please sign in instead.");
        }

        //register user
        boolean registered = registerUser(firstName, lastName, email, password, securityAnswer1, securityAnswer2, securityAnswer3, emergencyNumber);

        if (!registered) {
            return RegistrationResult.error("Error creating account. Please try again.");
        }

        return RegistrationResult.success("Account created successfully!");
    }

    private boolean isNullOrEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }


    private boolean registerUser(
            String firstName,
            String lastName,
            String email,
            String password,
            String securityAnswer1,
            String securityAnswer2,
            String securityAnswer3,
            String emergencyNumber
    ) {
        // Hash the password with BCrypt
        String hashedPassword = SecurityUtils.hashPassword(password);

        if (hashedPassword == null) {
            return false;
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
        editor.putString("emergencyContact" + email, emergencyNumber);
        editor.putInt("audio_alert", 0);
        editor.putInt("visual_alert", 0);
        editor.apply();

        return true;
    }

    public static class RegistrationResult {
        public final boolean success;
        public final String message;
        public final boolean emailAlreadyRegistered;

        private RegistrationResult(boolean success, String message, boolean emailAlreadyRegistered) {
            this.success = success;
            this.message = message;
            this.emailAlreadyRegistered = emailAlreadyRegistered;
        }

        public static RegistrationResult success(String message) {
            return new RegistrationResult(true, message, false);
        }

        public static RegistrationResult error(String message) {
            return new RegistrationResult(false, message, false);
        }

        public static RegistrationResult emailAlreadyRegistered(String message) {
            return new RegistrationResult(false, message, true);
        }
    }
}
