package com.hotelreservation.controller.kiosk;

import com.hotelreservation.service.OccupancyService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class OccupancyController {

    @FXML
    private Button rulesButton;

    @FXML
    private ComboBox<Integer> adultsComboBox;

    @FXML
    private ComboBox<Integer> childrenComboBox;

    @FXML
    private Label totalGuestsLabel;

    private final OccupancyService occupancyService = new OccupancyService();

    @FXML
    private void initialize() {
        adultsComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(1, 20).boxed().collect(Collectors.toList())
        ));
        childrenComboBox.setItems(FXCollections.observableArrayList(
                IntStream.rangeClosed(0, 20).boxed().collect(Collectors.toList())
        ));

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

        SceneNavigator.switchTo("/views/kiosk/StayDatesView.fxml");
    }

    @FXML
    private void backToWelcome() {
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
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
        alert.setTitle("Invalid Occupancy");
        alert.setHeaderText("Please check guest count");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
