package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import com.hotelreservation.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class GuestDetailsController {

    @FXML
    private Button rulesButton;

    @FXML
    private TextField firstNameField;

    @FXML
    private TextField lastNameField;

    @FXML
    private TextField emailField;

    @FXML
    private TextField phoneField;

    @FXML
    private TextArea addressArea;

    @FXML
    private Label firstNameErrorLabel;

    @FXML
    private Label lastNameErrorLabel;

    @FXML
    private Label emailErrorLabel;

    @FXML
    private Label phoneErrorLabel;

    @FXML
    private Label addressErrorLabel;

    @FXML
    private void initialize() {

        if (!BookingSession.getFirstName().isBlank()) {
            firstNameField.setText(BookingSession.getFirstName());
        }

        if (!BookingSession.getLastName().isBlank()) {
            lastNameField.setText(BookingSession.getLastName());
        }

        if (!BookingSession.getEmail().isBlank()) {
            emailField.setText(BookingSession.getEmail());
        }

        if (!BookingSession.getPhone().isBlank()) {
            phoneField.setText(BookingSession.getPhone());
        }

        if (!BookingSession.getAddress().isBlank()) {
            addressArea.setText(BookingSession.getAddress());
        }
    }

    @FXML
    private void continueToRoomSelection() {
        if (!isFormValid()) {
            return;
        }

        BookingSession.setFirstName(firstNameField.getText().trim());
        BookingSession.setLastName(lastNameField.getText().trim());
        BookingSession.setEmail(emailField.getText().trim());
        BookingSession.setPhone(phoneField.getText().trim());
        BookingSession.setAddress(addressArea.getText().trim());

        SceneNavigator.switchTo("/views/kiosk/RoomSelectionView.fxml");
    }

    @FXML
    private void backToStayDates() {
        SceneNavigator.switchTo("/views/kiosk/StayDatesView.fxml");
    }

    private boolean isFormValid() {
        String firstName = firstNameField.getText().trim();
        String lastName = lastNameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();
        String address = addressArea.getText().trim();

        boolean valid = true;

        if (!ValidationUtil.isValidName(firstName)) {
            showFieldError(firstNameErrorLabel, "Please enter a valid first name.");
            valid = false;
        } else {
            clearFieldError(firstNameErrorLabel);
        }

        if (!ValidationUtil.isValidName(lastName)) {
            showFieldError(lastNameErrorLabel, "Please enter a valid last name.");
            valid = false;
        } else {
            clearFieldError(lastNameErrorLabel);
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showFieldError(emailErrorLabel, "Please enter a valid email address.");
            valid = false;
        } else {
            clearFieldError(emailErrorLabel);
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            showFieldError(phoneErrorLabel, "Please enter a valid phone number.");
            valid = false;
        } else {
            clearFieldError(phoneErrorLabel);
        }

        if (!ValidationUtil.isValidAddress(address)) {
            showFieldError(addressErrorLabel, "Please enter a valid address.");
            valid = false;
        } else {
            clearFieldError(addressErrorLabel);
        }

        return valid;
    }

    private void showFieldError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearFieldError(Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    @FXML
    private void showRulesAndRegulations() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rules & Regulations");
        alert.setHeaderText("Hotel Rules & Regulations");
        alert.setContentText("Placeholder: rules and regulations content will be added later.");
        alert.showAndWait();
    }
}
