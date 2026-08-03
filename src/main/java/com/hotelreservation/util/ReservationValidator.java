package com.hotelreservation.util;

import java.time.LocalDate;

/**
 * Shared reservation field validation used by admin phone booking and kiosk booking.
 * Rules and messages match the former ReservationManagementService.validateReservationRequest.
 */
public final class ReservationValidator {

    private ReservationValidator() {
        // Prevent instantiation
    }

    public static void validate(
            String firstName,
            String lastName,
            String email,
            String phone,
            String address,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numAdults,
            int numChildren,
            boolean roomsSelected
    ) {
        if (!ValidationUtil.isValidName(firstName)) {
            throw new IllegalArgumentException(
                    "Please enter a valid first name."
            );
        }

        if (!ValidationUtil.isValidName(lastName)) {
            throw new IllegalArgumentException(
                    "Please enter a valid last name."
            );
        }

        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException(
                    "Please enter a valid email address."
            );
        }

        if (normalize(email).length() > 100) {
            throw new IllegalArgumentException(
                    "Email address cannot exceed 100 characters."
            );
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            throw new IllegalArgumentException(
                    "Please enter a valid phone number."
            );
        }

        if (!ValidationUtil.isValidAddress(address)) {
            throw new IllegalArgumentException(
                    "Please enter an address with at least five characters."
            );
        }

        if (normalize(address).length() > 255) {
            throw new IllegalArgumentException(
                    "Address cannot exceed 255 characters."
            );
        }

        validateDateOrder(checkInDate, checkOutDate);

        if (checkInDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Check-in date cannot be before today."
            );
        }

        if (numAdults < 1 || numAdults > 20) {
            throw new IllegalArgumentException(
                    "Adults must be between 1 and 20."
            );
        }

        if (numChildren < 0 || numChildren > 20) {
            throw new IllegalArgumentException(
                    "Children must be between 0 and 20."
            );
        }

        if (!roomsSelected) {
            throw new IllegalArgumentException(
                    "Select at least one available room."
            );
        }
    }

    public static void validateDateOrder(
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (checkInDate == null || checkOutDate == null) {
            throw new IllegalArgumentException(
                    "Check-in and check-out dates are required."
            );
        }

        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException(
                    "Check-out date must be after check-in date."
            );
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
