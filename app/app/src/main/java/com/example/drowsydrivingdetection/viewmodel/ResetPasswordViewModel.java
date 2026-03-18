package com.example.drowsydrivingdetection.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.drowsydrivingdetection.security.PasswordValidator;
import com.example.drowsydrivingdetection.security.SecurityUtils;

public class ResetPasswordViewModel extends AndroidViewModel {

    private final SharedPreferences sharedPreferences;

    public ResetPasswordViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("DrowsyDriverPrefs", Application.MODE_PRIVATE);
    }

    public ResetResult handleResetPassword(String newPassword, String confirmPassword) {
        if (newPassword == null || newPassword.trim().isEmpty()) {
            return ResetResult.fieldError("Please enter a new password", FocusField.NEW_PASSWORD);
        }

        if (confirmPassword == null || confirmPassword.trim().isEmpty()) {
            return ResetResult.fieldError("Please confirm your password", FocusField.CONFIRM_PASSWORD);
        }

        if (!newPassword.equals(confirmPassword)) {
            return ResetResult.fieldError("Passwords do not match", FocusField.CONFIRM_PASSWORD);
        }

        PasswordValidator.Result passwordResult = PasswordValidator.validate(newPassword);
        if (!passwordResult.isValid) {
            return ResetResult.passwordPolicyError(passwordResult.message);
        }

        String hashedPassword = SecurityUtils.hashPassword(newPassword);
        if (hashedPassword == null) {
            return ResetResult.error("Error. Please try again.");
        }

        sharedPreferences.edit()
                .putString("password_hash", hashedPassword)
                .apply();

        return ResetResult.success("Password Was Changed!");
    }

    public enum FocusField {
        NEW_PASSWORD, CONFIRM_PASSWORD
    }

    public static class ResetResult {
        public final boolean success;
        public final String message;
        public final FocusField focusField;
        public final boolean isPasswordPolicyError;

        private ResetResult(boolean success, String message, FocusField focusField, boolean isPasswordPolicyError) {
            this.success = success;
            this.message = message;
            this.focusField = focusField;
            this.isPasswordPolicyError = isPasswordPolicyError;
        }

        public static ResetResult success(String message) {
            return new ResetResult(true, message, null, false);
        }

        public static ResetResult error(String message) {
            return new ResetResult(false, message, null, false);
        }

        public static ResetResult fieldError(String message, FocusField field) {
            return new ResetResult(false, message, field, false);
        }

        public static ResetResult passwordPolicyError(String message) {
            return new ResetResult(false, message, FocusField.NEW_PASSWORD, true);
        }
    }
}
