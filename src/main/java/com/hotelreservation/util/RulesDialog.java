package com.hotelreservation.util;

import javafx.scene.control.Alert;

public final class RulesDialog {

    private RulesDialog() {
    }

    public static void show() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rules & Regulations");
        alert.setHeaderText("Hotel Booking Policy");
        alert.setContentText(
                """
                • A valid adult guest and accurate contact information are required.
                • Check-in begins at 3:00 PM and checkout is by 11:00 AM.
                • Every selected room must have at least one assigned guest.
                • Single, Deluxe, and Penthouse rooms allow up to 2 guests.
                • Double rooms allow up to 4 guests.
                • Weekend and peak-season pricing may apply.
                • Cancellations are subject to the hotel's cancellation policy.
                • Smoking and damage charges may be added after inspection.
                • Payment is completed with the front desk before checkout.
                """
        );
        alert.getDialogPane().setMinWidth(560);
        alert.showAndWait();
    }
}
