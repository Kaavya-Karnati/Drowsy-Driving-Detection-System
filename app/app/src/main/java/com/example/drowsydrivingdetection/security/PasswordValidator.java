package com.example.drowsydrivingdetection.security;

import java.util.regex.Pattern;

public final class PasswordValidator {

    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*].*");
    private static final int MIN_PASSWORD_LENGTH = 8;

    private PasswordValidator() {}

    public static Result validate(String password) {
        // Validate password

        // Check minimum length
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            return Result.invalid("Password must be at least " + MIN_PASSWORD_LENGTH + " characters long");
        }

        // Check for at least one digit
        if (!DIGIT_PATTERN.matcher(password).matches()) {
            return Result.invalid("Password must contain at least one digit (0-9)");
        }

        // Check for at least one special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            return Result.invalid("Password must contain at least one special character (!@#$%^&*)");
        }

        return Result.valid();
    }

    public static class Result {
        public final boolean isValid;
        public final String message;

        private Result(boolean isValid, String message) {
            this.isValid = isValid;
            this.message = message;
        }

        static Result valid() {
            return new Result(true, null);
        }

        static Result invalid(String message) {
            return new Result(false, message);
        }
    }
}
