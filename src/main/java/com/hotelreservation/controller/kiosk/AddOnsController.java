package com.hotelreservation.controller.kiosk;

import com.hotelreservation.service.PricingService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.RulesDialog;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;

public class AddOnsController {

    @FXML
    private Button rulesButton;

    @FXML
    private CheckBox wifiCheckBox;

    @FXML
    private CheckBox breakfastCheckBox;

    @FXML
    private CheckBox spaCheckBox;

    @FXML
    private CheckBox parkingCheckBox;

    @FXML
    private CheckBox laundryCheckBox;

    @FXML
    private CheckBox airportPickupCheckBox;

    @FXML
    private Label roomTotalLabel;

    @FXML
    private Label addOnTotalLabel;

    @FXML
    private Label subtotalLabel;

    private final PricingService pricingService;

    public AddOnsController(PricingService pricingService) {
        this.pricingService = pricingService;
    }

    @FXML
    private void initialize() {
        wifiCheckBox.setSelected(BookingSession.isWifiSelected());
        breakfastCheckBox.setSelected(BookingSession.isBreakfastSelected());
        spaCheckBox.setSelected(BookingSession.isSpaSelected());
        parkingCheckBox.setSelected(BookingSession.isParkingSelected());
        laundryCheckBox.setSelected(BookingSession.isLaundrySelected());
        airportPickupCheckBox.setSelected(BookingSession.isAirportPickupSelected());
        updateCostPreview();
    }

    @FXML
    private void backToRoomSelection() {
        SceneNavigator.switchTo("/views/kiosk/RoomSelectionView.fxml");
    }

    @FXML
    private void continueToLoyaltyCheck() {
        BookingSession.setWifiSelected(wifiCheckBox.isSelected());
        BookingSession.setBreakfastSelected(breakfastCheckBox.isSelected());
        BookingSession.setSpaSelected(spaCheckBox.isSelected());
        BookingSession.setParkingSelected(parkingCheckBox.isSelected());
        BookingSession.setLaundrySelected(laundryCheckBox.isSelected());
        BookingSession.setAirportPickupSelected(airportPickupCheckBox.isSelected());
        SceneNavigator.switchTo("/views/kiosk/LoyaltyCheckView.fxml");
    }

    @FXML
    private void updateCostPreview() {
        double roomTotal = pricingService.calculateRoomTotal();
        double addOnTotal = pricingService.calculateAddOnTotal(
                wifiCheckBox.isSelected(),
                breakfastCheckBox.isSelected(),
                spaCheckBox.isSelected(),
                parkingCheckBox.isSelected(),
                laundryCheckBox.isSelected(),
                airportPickupCheckBox.isSelected()
        );
        double subtotal = pricingService.calculateSubtotal(roomTotal, addOnTotal);

        roomTotalLabel.setText(
                String.format("Room Total: CAD %.2f", roomTotal));

        addOnTotalLabel.setText(
                String.format("Add-ons Total: CAD %.2f", addOnTotal));

        subtotalLabel.setText(
                String.format("Estimated Subtotal: CAD %.2f", subtotal));
    }

    @FXML
    private void showRulesAndRegulations() {
        RulesDialog.show();
    }
}
