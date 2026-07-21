package com.hotelreservation.controller.kiosk;

import com.hotelreservation.model.RoomType;
import com.hotelreservation.service.OccupancyService;
import com.hotelreservation.service.RoomAvailabilityService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class RoomSelectionController {

    @FXML
    private Button rulesButton;

    @FXML
    private Label totalGuestsLabel;

    @FXML
    private Label singleQtyLabel;

    @FXML
    private Label doubleQtyLabel;

    @FXML
    private Label deluxeQtyLabel;

    @FXML
    private Label penthouseQtyLabel;

    @FXML
    private Label capacityLabel;

    @FXML
    private Label roomPlanLabel;

    @FXML
    private Label validationLabel;

    private final OccupancyService occupancyService = new OccupancyService();
    private final RoomAvailabilityService roomAvailabilityService = new RoomAvailabilityService();

    private int singleQty;
    private int doubleQty;
    private int deluxeQty;
    private int penthouseQty;

    @FXML
    private void initialize() {
        singleQty = BookingSession.getSingleRoomQuantity();
        doubleQty = BookingSession.getDoubleRoomQuantity();
        deluxeQty = BookingSession.getDeluxeRoomQuantity();
        penthouseQty = BookingSession.getPenthouseRoomQuantity();

        if (BookingSession.getTotalRoomQuantity() == 0) {
            suggestRoomPlan();
        }

        updateRoomSelectionDisplay();
    }

    private void suggestRoomPlan() {
        int totalGuests = occupancyService.getTotalGuests(
                BookingSession.getNumAdults(),
                BookingSession.getNumChildren()
        );
        singleQty = 0;
        doubleQty = 0;
        deluxeQty = 0;
        penthouseQty = 0;

        if (totalGuests <= RoomType.SINGLE.getMaxOccupancy()) {
            singleQty = 1;
        } else if (totalGuests <= RoomType.DOUBLE.getMaxOccupancy()) {
            doubleQty = 1;
        } else {
            doubleQty = (int) Math.ceil(totalGuests / (double) RoomType.DOUBLE.getMaxOccupancy());
        }
    }

    @FXML
    private void increaseSingle() {
        singleQty++;
        updateRoomSelectionDisplay();
    }

    @FXML
    private void decreaseSingle() {
        if (singleQty > 0) {
            singleQty--;
        }
        updateRoomSelectionDisplay();
    }

    @FXML
    private void increaseDouble() {
        doubleQty++;
        updateRoomSelectionDisplay();
    }

    @FXML
    private void decreaseDouble() {
        if (doubleQty > 0) {
            doubleQty--;
        }
        updateRoomSelectionDisplay();
    }

    @FXML
    private void increaseDeluxe() {
        deluxeQty++;
        updateRoomSelectionDisplay();
    }

    @FXML
    private void decreaseDeluxe() {
        if (deluxeQty > 0) {
            deluxeQty--;
        }
        updateRoomSelectionDisplay();
    }

    @FXML
    private void increasePenthouse() {
        penthouseQty++;
        updateRoomSelectionDisplay();
    }

    @FXML
    private void decreasePenthouse() {
        if (penthouseQty > 0) {
            penthouseQty--;
        }
        updateRoomSelectionDisplay();
    }

    private void updateRoomSelectionDisplay() {
        int totalGuests = occupancyService.getTotalGuests(
                BookingSession.getNumAdults(),
                BookingSession.getNumChildren()
        );
        int totalCapacity = calculateTotalCapacity();

        totalGuestsLabel.setText("Total Guests: " + totalGuests);

        singleQtyLabel.setText(String.valueOf(singleQty));
        doubleQtyLabel.setText(String.valueOf(doubleQty));
        deluxeQtyLabel.setText(String.valueOf(deluxeQty));
        penthouseQtyLabel.setText(String.valueOf(penthouseQty));

        capacityLabel.setText("Selected Room Capacity: " + totalCapacity);

        roomPlanLabel.setText(buildRoomPlanSummary());

        if (getTotalRoomQuantity() == 0) {
            validationLabel.setText("Please select at least one room.");
        } else if (totalCapacity < totalGuests) {
            validationLabel.setText("Selected rooms do not have enough capacity for all guests.");
        } else {
            validationLabel.setText("Room selection is valid.");
        }
    }

    private int calculateTotalCapacity() {
        return (singleQty * RoomType.SINGLE.getMaxOccupancy())
                + (doubleQty * RoomType.DOUBLE.getMaxOccupancy())
                + (deluxeQty * RoomType.DELUXE.getMaxOccupancy())
                + (penthouseQty * RoomType.PENTHOUSE.getMaxOccupancy());
    }

    private int getTotalRoomQuantity() {
        return singleQty + doubleQty + deluxeQty + penthouseQty;
    }

    private String buildRoomPlanSummary() {
        StringBuilder summary = new StringBuilder();

        if (singleQty > 0) {
            summary.append(singleQty).append(" Single Room(s)  ");
        }

        if (doubleQty > 0) {
            summary.append(doubleQty).append(" Double Room(s)  ");
        }

        if (deluxeQty > 0) {
            summary.append(deluxeQty).append(" Deluxe Room(s)  ");
        }

        if (penthouseQty > 0) {
            summary.append(penthouseQty).append(" Penthouse Room(s)");
        }

        if (summary.isEmpty()) {
            return "No rooms selected";
        }

        return summary.toString().trim();
    }

    @FXML
    private void continueToAddOns() {
        int totalGuests = occupancyService.getTotalGuests(
                BookingSession.getNumAdults(),
                BookingSession.getNumChildren()
        );
        int totalCapacity = calculateTotalCapacity();

        if (getTotalRoomQuantity() == 0) {
            showError("Please select at least one room.");
            return;
        }

        if (totalCapacity < totalGuests) {
            showError("Selected rooms do not have enough capacity for all guests.");
            return;
        }

        String availabilityError = roomAvailabilityService.validateRequestedQuantities(
                singleQty,
                doubleQty,
                deluxeQty,
                penthouseQty,
                BookingSession.getCheckInDate(),
                BookingSession.getCheckOutDate()
        );

        if (availabilityError != null) {
            showError(availabilityError);
            return;
        }

        BookingSession.setSingleRoomQuantity(singleQty);
        BookingSession.setDoubleRoomQuantity(doubleQty);
        BookingSession.setDeluxeRoomQuantity(deluxeQty);
        BookingSession.setPenthouseRoomQuantity(penthouseQty);
        BookingSession.setGroupBooking(BookingSession.getNumAdults() >= 3);

        if (singleQty > 0) {
            BookingSession.setSelectedRoomType(RoomType.SINGLE);
        } else if (doubleQty > 0) {
            BookingSession.setSelectedRoomType(RoomType.DOUBLE);
        } else if (deluxeQty > 0) {
            BookingSession.setSelectedRoomType(RoomType.DELUXE);
        } else if (penthouseQty > 0) {
            BookingSession.setSelectedRoomType(RoomType.PENTHOUSE);
        }

        SceneNavigator.switchTo("/views/kiosk/AddOnsView.fxml");
    }

    @FXML
    private void backToGuestDetails() {
        SceneNavigator.switchTo("/views/kiosk/GuestDetailsView.fxml");
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
        alert.setTitle("Invalid Room Selection");
        alert.setHeaderText("Please check selected rooms");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
