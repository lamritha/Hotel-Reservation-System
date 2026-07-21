package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import com.hotelreservation.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class LoyaltyCheckController {

    @FXML
    private Button rulesButton;

    @FXML
    private TextField phoneField;

    @FXML
    private Label loyaltyStatusLabel;

    @FXML
    private Label loyaltyPointsLabel;

    @FXML
    private void initialize() {
        phoneField.setText(BookingSession.getPhone());
        loyaltyStatusLabel.setText("Phone number loaded from guest details.");
        loyaltyPointsLabel.setText("Click Check Loyalty to check available points.");
    }

    @FXML
    private void checkLoyalty() {
        String phone = phoneField.getText().trim();

        if (phone.isEmpty()) {
            showError("Please enter a phone number.");
            return;
        }

        BookingSession.setPhone(phone);

        // Demo logic for prototype:
        // This simulates checking if the guest is an existing customer.
        if (phone.equals("416-555-0198") || phone.equals("4165550198")) {
            loyaltyStatusLabel.setText("Existing customer found.");
            loyaltyPointsLabel.setText("Available Loyalty Points: 420");
        } else {
            loyaltyStatusLabel.setText("No existing loyalty account found.");
            loyaltyPointsLabel.setText("Guest can continue without loyalty points.");
        }
    }

    @FXML
    private void continueToBookingSummary() {
        String phone = phoneField.getText().trim();

        if (!ValidationUtil.isValidPhone(phone)) {
            showError("Please enter a valid phone number.");
            return;
        }

        BookingSession.setPhone(phone);

        System.out.println("Loyalty check completed:");
        System.out.println("Phone: " + BookingSession.getPhone());

        SceneNavigator.switchTo("/views/kiosk/BookingSummaryView.fxml");
    }

    @FXML
    private void backToAddOns() {
        SceneNavigator.switchTo("/views/kiosk/AddOnsView.fxml");
    }

    @FXML
    private void showRulesAndRegulations() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rules & Regulations");
        alert.setHeaderText("Hotel Rules & Regulations");
        alert.setContentText("Placeholder: rules and regulations content will be added later.");
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Phone Required");
        alert.setHeaderText("Please check the phone number");
        alert.setContentText(message);
        alert.showAndWait();
    }
}