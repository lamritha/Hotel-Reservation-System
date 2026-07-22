package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class FeedbackLookupController {

    private static final String DEMO_RESERVATION_ID = "RES-1001";

    @FXML
    private TextField lookupField;

    @FXML
    private Label statusLabel;

    @FXML
    private void checkFeedbackEligibility() {
        String input = lookupField.getText().trim();

        if (input.isEmpty()) {
            showError("Please enter a reservation ID or phone number.");
            return;
        }

        /*
         * Demo logic for Milestone 2 prototype:
         * This simulates checking whether the guest has checked out.
         *
         * Later we will replace this with a real database check:
         * ReservationStatus == CHECKED_OUT
         */
        if (input.equalsIgnoreCase(DEMO_RESERVATION_ID) || input.equals("4165550198") || input.equals("416-555-0198")) {
            statusLabel.setText("Guest has checked out. Feedback form is available.");
            BookingSession.setFeedbackReservationId(DEMO_RESERVATION_ID);
            SceneNavigator.switchTo("/views/kiosk/FeedbackView.fxml");
        } else {
            statusLabel.setText("Feedback is only available after checkout.");
            showError("This reservation is not checked out yet. Feedback cannot be submitted before checkout.");
        }
    }

    @FXML
    private void backToWelcome() {
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Feedback Not Available");
        alert.setHeaderText("Checkout Required");
        alert.setContentText(message);
        alert.showAndWait();
    }
}