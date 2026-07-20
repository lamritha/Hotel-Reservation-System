package com.hotelreservation.util;

import java.util.regex.Pattern;

public final class ValidationUtil {

    private ValidationUtil() {
        // Prevent instantiation
    }

    // Letters, spaces, apostrophes, hyphens
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z\\s'-]{1,49}$");

    // Simple email validation
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Accepts:
    // 4165551234
    // 416-555-1234
    // (416) 555-1234
    // 416 555 1234
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\(\\d{3}\\)|\\d{3})[- ]?\\d{3}[- ]?\\d{4}$");

    public static boolean isValidName(String name) {
        if (name == null) {
            return false;
        }

        return NAME_PATTERN.matcher(name.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }

        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        if (phone == null) {
            return false;
        }

        return PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidAddress(String address) {
        return address != null && address.trim().length() >= 5;
    }
}