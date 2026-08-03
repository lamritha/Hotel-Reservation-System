package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.RulesDialog;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StayDatesController {

    @FXML
    private Button rulesButton;

    @FXML
    private DatePicker checkInDatePicker;

    @FXML
    private DatePicker checkOutDatePicker;

    @FXML
    private Label validationLabel;

    @FXML
    private Label nightsLabel;

    @FXML
    private void initialize() {

        if (BookingSession.getCheckInDate() != null &&
                BookingSession.getCheckOutDate() != null) {

            checkInDatePicker.setValue(BookingSession.getCheckInDate());
            checkOutDatePicker.setValue(BookingSession.getCheckOutDate());

        } else {

            checkInDatePicker.setValue(LocalDate.now().plusDays(1));
            checkOutDatePicker.setValue(LocalDate.now().plusDays(2));

        }

        validateDates();
    }

    @FXML
    private void validateDates() {
        LocalDate checkIn = checkInDatePicker.getValue();
        LocalDate checkOut = checkOutDatePicker.getValue();

        if (checkIn == null || checkOut == null) {
            validationLabel.setText("Please select both check-in and check-out dates.");
            nightsLabel.setText("Nights: 0");
            return;
        }

        if (checkIn.isBefore(LocalDate.now())) {
            validationLabel.setText("Invalid dates: check-in cannot be in the past.");
            nightsLabel.setText("Nights: 0");
            return;
        }

        if (!checkOut.isAfter(checkIn)) {
            validationLabel.setText("Invalid dates: check-out must be after check-in.");
            nightsLabel.setText("Nights: 0");
            return;
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);

        validationLabel.setText("Dates are valid.");
        nightsLabel.setText("Nights: " + nights);
    }

    @FXML
    private void continueToGuestDetails() {
        LocalDate checkIn = checkInDatePicker.getValue();
        LocalDate checkOut = checkOutDatePicker.getValue();

        if (checkIn == null || checkOut == null) {
            showError("Please select both check-in and check-out dates.");
            return;
        }

        if (checkIn.isBefore(LocalDate.now())) {
            showError("Check-in date cannot be in the past.");
            return;
        }

        if (!checkOut.isAfter(checkIn)) {
            showError("Check-out date must be after check-in date.");
            return;
        }

        BookingSession.setCheckInDate(checkIn);
        BookingSession.setCheckOutDate(checkOut);

        SceneNavigator.switchTo("/views/kiosk/GuestDetailsView.fxml");
    }

    @FXML
    private void backToOccupancy() {
        SceneNavigator.switchTo("/views/kiosk/OccupancyView.fxml");
    }

    @FXML
    private void showRulesAndRegulations() {
        RulesDialog.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Dates");
        alert.setHeaderText("Please check stay dates");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
