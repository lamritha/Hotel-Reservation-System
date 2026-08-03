package com.hotelreservation.security;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordService {

    private static final int BCRYPT_LOG_ROUNDS = 12;

    public String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        return BCrypt.hashpw(
                rawPassword,
                BCrypt.gensalt(BCRYPT_LOG_ROUNDS)
        );
    }

    public boolean matches(String rawPassword, String passwordHash) {
        if (rawPassword == null || rawPassword.isEmpty()
                || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }

        try {
            return BCrypt.checkpw(rawPassword, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}