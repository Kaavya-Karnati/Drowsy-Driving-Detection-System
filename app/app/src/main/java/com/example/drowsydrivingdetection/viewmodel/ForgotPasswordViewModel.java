package com.example.drowsydrivingdetection.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class ForgotPasswordViewModel extends AndroidViewModel {

    private static final int MAX_ATTEMPTS = 3;
    private static final long LOCK_DURATION_MS = 60 * 60 * 1000;

    private final SharedPreferences sharedPreferences;

    public ForgotPasswordViewModel(@NonNull Application application) {
        super(application);
        sharedPreferences = application.getSharedPreferences("DrowsyDriverPrefs", Application.MODE_PRIVATE);
    }

    public VerificationResult handleVerification(
            String firstName,
            String lastName,
            String email,
            String answer1,
            String answer2,
            String answer3
    ) {
        if (isEmpty(firstName)) {
            return VerificationResult.fieldError("Please enter your first name", FocusField.FIRST_NAME);
        }

        if (isEmpty(lastName)) {
            return VerificationResult.fieldError("Please enter your last name", FocusField.LAST_NAME);
        }

        if (isEmpty(email)) {
            return VerificationResult.fieldError("Please enter your email", FocusField.EMAIL);
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            return VerificationResult.fieldError("Enter email address in the right format", FocusField.EMAIL);
        }

        if (isEmpty(answer1)) {
            return VerificationResult.fieldError("Please answer Security Question 1", FocusField.ANSWER1);
        }

        if (isEmpty(answer2)) {
            return VerificationResult.fieldError("Please answer Security Question 2", FocusField.ANSWER2);
        }

        if (isEmpty(answer3)) {
            return VerificationResult.fieldError("Please answer Security Question 3", FocusField.ANSWER3);
        }

        String storedEmail = sharedPreferences.getString("registered_email", null);
        String storedFirstName = storedEmail != null ? sharedPreferences.getString("userFirstName_" + storedEmail, null) : null;
        String storedLastName = storedEmail != null ? sharedPreferences.getString("userLastName_" + storedEmail, null) : null;

        if (storedFirstName == null || storedLastName == null || storedEmail == null) {
            return VerificationResult.error("Account doesn't exist");
        }

        if (!storedEmail.equalsIgnoreCase(email)) {
            return VerificationResult.fieldError("Invalid details", FocusField.EMAIL);
        }

        String attemptKey = "resetattempts" + email;
        String lockKey = "resetlocktime" + email;

        long lockTime = sharedPreferences.getLong(lockKey, 0);
        if (System.currentTimeMillis() < lockTime) {
            long remainingMinutes = ((lockTime - System.currentTimeMillis()) / 60000) + 1;
            return VerificationResult.lockedOut("Too many attempts. Try again in " + remainingMinutes + " minutes.");
        }

        if (!storedFirstName.equalsIgnoreCase(firstName)) {
            return VerificationResult.fieldError("Invalid details", FocusField.FIRST_NAME);
        }

        if (!storedLastName.equalsIgnoreCase(lastName)) {
            return VerificationResult.fieldError("Invalid details", FocusField.LAST_NAME);
        }

        String s1 = sharedPreferences.getString("securityAnswer1", "").trim();
        String s2 = sharedPreferences.getString("securityAnswer2", "").trim();
        String s3 = sharedPreferences.getString("securityAnswer3", "").trim();

        int correct = 0;
        if (answer1.equalsIgnoreCase(s1)) correct++;
        if (answer2.equalsIgnoreCase(s2)) correct++;
        if (answer3.equalsIgnoreCase(s3)) correct++;

        if (correct >= 2) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(attemptKey, 0);
            editor.putLong(lockKey, 0);
            editor.apply();
            return VerificationResult.success("Passed!");
        } else {
            return recordFailedAttempt(email);
        }
    }

    private VerificationResult recordFailedAttempt(String email) {
        String attemptKey = "resetattempts" + email;
        String lockKey = "resetlocktime" + email;

        int attempts = sharedPreferences.getInt(attemptKey, 0) + 1;

        if (attempts >= MAX_ATTEMPTS) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putLong(lockKey, System.currentTimeMillis() + LOCK_DURATION_MS);
            editor.putInt(attemptKey, 0);
            editor.apply();
            return VerificationResult.lockedOut("Too many failed attempts. Locked for 1 hour.");
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(attemptKey, attempts);
            editor.apply();
            int remaining = MAX_ATTEMPTS - attempts;
            return VerificationResult.error("Wrong answer! " + remaining + " attempts remaining.");
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    public enum FocusField {
        FIRST_NAME, LAST_NAME, EMAIL, ANSWER1, ANSWER2, ANSWER3
    }

    public static class VerificationResult {
        public final boolean success;
        public final String message;
        public final boolean lockedOut;
        public final FocusField focusField;

        private VerificationResult(boolean success, String message, boolean lockedOut, FocusField focusField) {
            this.success = success;
            this.message = message;
            this.lockedOut = lockedOut;
            this.focusField = focusField;
        }

        public static VerificationResult success(String message) {
            return new VerificationResult(true, message, false, null);
        }

        public static VerificationResult error(String message) {
            return new VerificationResult(false, message, false, null);
        }

        public static VerificationResult fieldError(String message, FocusField field) {
            return new VerificationResult(false, message, false, field);
        }

        public static VerificationResult lockedOut(String message) {
            return new VerificationResult(false, message, true, null);
        }
    }
}
