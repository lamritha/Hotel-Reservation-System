package com.hotelreservation.controller.kiosk;

import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ConfirmationController {

    @FXML
    private Label reservationIdLabel;

    @FXML
    private Label guestNameLabel;

    @FXML
    private Label contactLabel;

    @FXML
    private Label stayDatesLabel;

    @FXML
    private Label occupancyLabel;

    @FXML
    private Label roomPlanLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label paymentNoticeLabel;

    @FXML
    private void initialize() {
        if (BookingSession.getSavedReservationId() != null) {
            reservationIdLabel.setText("Reservation ID: RES-" + BookingSession.getSavedReservationId());
        } else {
            reservationIdLabel.setText("Reservation ID: Pending database save");
        }

        guestNameLabel.setText(
                "Guest: " + BookingSession.getFirstName() + " " + BookingSession.getLastName()
        );

        contactLabel.setText(
                "Email: " + BookingSession.getEmail()
                        + " | Phone: " + BookingSession.getPhone()
        );

        stayDatesLabel.setText(
                "Stay Dates: " + BookingSession.getCheckInDate()
                        + " to " + BookingSession.getCheckOutDate()
        );

        occupancyLabel.setText(
                "Adults: " + BookingSession.getNumAdults()
                        + " | Children: " + BookingSession.getNumChildren()
                        + " | Total Guests: " + BookingSession.getTotalGuests()
        );

        roomPlanLabel.setText(
                "Room Plan: " + BookingSession.getRoomPlanSummary()
        );

        statusLabel.setText("Reservation Status: Pending Front Desk Payment");

        paymentNoticeLabel.setText(
                "Payment is not collected at the kiosk. The guest must complete payment at the front desk with admin."
        );
    }

    @FXML
    private void backToHome() {
        BookingSession.reset();
        SceneNavigator.switchTo("/views/kiosk/WelcomeView.fxml");
    }
}