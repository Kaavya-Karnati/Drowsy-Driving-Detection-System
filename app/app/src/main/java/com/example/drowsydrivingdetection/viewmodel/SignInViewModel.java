package com.example.drowsydrivingdetection.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.drowsydrivingdetection.security.PasswordValidator;
import com.example.drowsydrivingdetection.security.SecurityUtils;

public class SignInViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPreferences;

    public SignInViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("DrowsyDriverPrefs", Application.MODE_PRIVATE);
    }

    public boolean isUserLoggedIn() {
        return sharedPreferences.getBoolean("isLoggedIn", false);
    }

    public SignInResult handleEmailSignIn(String email, String password) {
        if (email == null || email.trim().isEmpty()) {
            return SignInResult.error("Please enter your email");
        }

        if (password == null || password.trim().isEmpty()) {
            return SignInResult.error("Please enter your password");
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return SignInResult.error("Please enter a valid email address");
        }

        PasswordValidator.Result passwordResult = PasswordValidator.validate(password);
        if (!passwordResult.isValid) {
            return SignInResult.error("Invalid Password");
        }

        String storedEmail = sharedPreferences.getString("registered_email", null);

        if (storedEmail == null) {
            return SignInResult.noAccount("No account found. Please register first.");
        } else if (storedEmail.equals(email)) {
            return loginExistingUser(email, password);
        } else {
            return SignInResult.error("Email not found. Please check your email or register.");
        }
    }

    private SignInResult loginExistingUser(String email, String password) {
        String storedHash = sharedPreferences.getString("password_hash", null);

        if (storedHash == null) {
            return SignInResult.error("Account error. Please register again.");
        }

        if (SecurityUtils.verifyPassword(password, storedHash)) {
            String fullName = sharedPreferences.getString("userName_" + email, "Guest User");
            if (fullName.isEmpty()) {
                fullName = "User";
            }

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("isLoggedIn", true);
            editor.putBoolean("isGuest", false);
            editor.putString("userEmail", email);
            editor.apply();

            return SignInResult.success("Sign in successful!");
        } else {
            return SignInResult.error("Incorrect password. Please try again.");
        }
    }

    public SignInResult handleGuestSignIn() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putBoolean("isGuest", true);
        editor.putString("userEmail", "Guest");
        editor.putString("userName", "Guest User");
        editor.putString("userFirstName", "Guest");
        editor.putString("userLastName", "User");
        editor.apply();

        return SignInResult.success("Continuing as guest");
    }

    public static class SignInResult {
        public final boolean success;
        public final String message;
        public final boolean noAccount;

        private SignInResult(boolean success, String message, boolean noAccount) {
            this.success = success;
            this.message = message;
            this.noAccount = noAccount;
        }

        public static SignInResult success(String message) {
            return new SignInResult(true, message, false);
        }

        public static SignInResult error(String message) {
            return new SignInResult(false, message, false);
        }

        public static SignInResult noAccount(String message) {
            return new SignInResult(false, message, true);
        }
    }
}
