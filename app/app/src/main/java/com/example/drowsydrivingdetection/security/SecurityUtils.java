package com.example.drowsydrivingdetection.security;

import org.mindrot.jbcrypt.BCrypt;

public class SecurityUtils {

    /**
     * Hashes a password using BCrypt
     * BCrypt automatically generates and includes the salt
     * @param password The password to hash
     * @return The hashed password (includes salt)
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    /**
     * Verifies a password against a BCrypt hash
     * @param password The password to verify
     * @param hashedPassword The stored BCrypt hash
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String password, String hashedPassword) {
        try {
            return BCrypt.checkpw(password, hashedPassword);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}