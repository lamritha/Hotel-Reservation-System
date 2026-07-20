package com.hotelreservation.controller.kiosk;

import com.hotelreservation.service.OccupancyService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

public class OccupancyController {

    @FXML
    private ComboBox<Integer> adultsComboBox;

    @FXML
    private ComboBox<Integer> childrenComboBox;

    @FXML
    private Label totalGuestsLabel;

    private final OccupancyService occupancyService = new OccupancyService();

    @FXML
    private void initialize() {
        adultsComboBox.setItems(FXCollections.observableArrayList(1, 2, 3, 4));
        childrenComboBox.setItems(FXCollections.observableArrayList(0, 1, 2, 3, 4));

        if (BookingSession.getNumAdults() > 0) {
            adultsComboBox.setValue(BookingSession.getNumAdults());
        } else {
            adultsComboBox.setValue(2);
        }

        childrenComboBox.setValue(BookingSession.getNumChildren());

        updateTotalGuests();
    }

    @FXML
    private void updateTotalGuests() {
        int adults = adultsComboBox.getValue() == null ? 0 : adultsComboBox.getValue();
        int children = childrenComboBox.getValue() == null ? 0 : childrenComboBox.getValue();

        int totalGuests = occupancyService.getTotalGuests(adults, children);
        totalGuestsLabel.setText("Total Guests: " + totalGuests);
    }

    @FXML
    private void continueToStayDates() {
        if (adultsComboBox.getValue() == null || childrenComboBox.getValue() == null) {
            showError("Please select the number of adults and children.");
            return;
        }

        int adults = adultsComboBox.getValue();
        int children = childrenComboBox.getValue();
        int totalGuests = occupancyService.getTotalGuests(adults, children);

        if (totalGuests <= 0) {
            showError("At least one guest is required.");
            return;
        }

        BookingSession.setNumAdults(adults);
        BookingSession.setNumChildren(children);

        System.out.println("Occupancy saved in BookingSession:");
        System.out.println("Adults: " + BookingSession.getNumAdults());
        System.out.println("Children: " + BookingSession.getNumChildren());
        System.out.println("Total Guests: " + totalGuests);

        SceneNavigator.switchTo("/views/kiosk/StayDatesView.fxml");
    }

    @FXML
    private void backToWelcome() {
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Invalid Occupancy");
        alert.setHeaderText("Please check guest count");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
