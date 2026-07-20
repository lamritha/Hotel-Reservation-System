package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import com.hotelreservation.util.ValidationUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class GuestDetailsController {

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

        System.out.println("Guest personal details saved in BookingSession:");
        System.out.println("Guest: " + BookingSession.getFirstName() + " " + BookingSession.getLastName());
        System.out.println("Email: " + BookingSession.getEmail());
        System.out.println("Phone: " + BookingSession.getPhone());
        System.out.println("Address: " + BookingSession.getAddress());

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

        if (!ValidationUtil.isValidName(firstName)) {
            showError("Please enter a valid first name.");
            return false;
        }

        if (!ValidationUtil.isValidName(lastName)) {
            showError("Please enter a valid last name.");
            return false;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            showError("Please enter a valid email address.");
            return false;
        }

        if (!ValidationUtil.isValidPhone(phone)) {
            showError("Please enter a valid phone number.");
            return false;
        }

        if (!ValidationUtil.isValidAddress(address)) {
            showError("Please enter a valid address.");
            return false;
        }

        return true;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Guest Details");
        alert.setHeaderText("Please check the form");
        alert.setContentText(message);
        alert.showAndWait();
    }
}