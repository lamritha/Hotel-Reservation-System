package com.hotelreservation.controller.kiosk;

import com.hotelreservation.model.Guest;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.OccupancyService;
import com.hotelreservation.service.PricingService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.RulesDialog;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class BookingSummaryController {

    @FXML
    private Button rulesButton;

    @FXML
    private Label selectedAddOnsLabel;

    @FXML
    private Label roomTotalLabel;

    @FXML
    private Label addOnTotalLabel;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label taxLabel;

    @FXML
    private Label loyaltyStatusLabel;

    @FXML
    private Label finalTotalLabel;

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
    private Label paymentNoticeLabel;

    private final OccupancyService occupancyService;
    private final PricingService pricingService;
    private final BookingService bookingService;

    public BookingSummaryController(
            OccupancyService occupancyService,
            PricingService pricingService,
            BookingService bookingService
    ) {
        this.occupancyService = occupancyService;
        this.pricingService = pricingService;
        this.bookingService = bookingService;
    }

    @FXML
    private void initialize() {
        PricingService.PriceBreakdown priceBreakdown = pricingService.calculateSessionPriceBreakdown();

        guestNameLabel.setText(
                "Guest: " + BookingSession.getFirstName() + " " + BookingSession.getLastName()
        );

        contactLabel.setText(
                "Email: " + BookingSession.getEmail()
                        + " | Phone: " + BookingSession.getPhone()
        );

        stayDatesLabel.setText(
                "Stay: " + BookingSession.getCheckInDate()
                        + " to " + BookingSession.getCheckOutDate()
        );

        int totalGuests = occupancyService.getTotalGuests(
                BookingSession.getNumAdults(),
                BookingSession.getNumChildren()
        );

        occupancyLabel.setText(
                "Adults: " + BookingSession.getNumAdults()
                        + " | Children: " + BookingSession.getNumChildren()
                        + " | Total Guests: " + totalGuests
        );

        roomPlanLabel.setText(
                "Room Plan: " + BookingSession.getRoomPlanSummary()
                        + " | Total Capacity: " + BookingSession.getTotalRoomCapacity()
        );

        paymentNoticeLabel.setText(
                "Payment will be completed at the front desk by admin after reservation confirmation."
        );

        selectedAddOnsLabel.setText(
                "Selected Add-ons: " + pricingService.getSelectedAddOnsSummaryFromSession());

        roomTotalLabel.setText(
                String.format("Room Total: CAD %.2f", priceBreakdown.getRoomTotal()));

        addOnTotalLabel.setText(
                String.format("Add-ons Total: CAD %.2f", priceBreakdown.getAddOnTotal()));

        subtotalLabel.setText(
                String.format("Subtotal: CAD %.2f", priceBreakdown.getSubtotal()));

        taxLabel.setText(
                String.format("Tax (13%%): CAD %.2f", priceBreakdown.getTaxAmount()));

        if (BookingSession.isLoyaltyEnrolled()) {
            loyaltyStatusLabel.setText(
                    "Loyalty Status: Member (" + BookingSession.getLoyaltyNumber()
                            + ", " + BookingSession.getLoyaltyPointsBalance() + " points)"
            );
        } else if (BookingSession
                .isLoyaltyEnrollmentRequested()) {
            loyaltyStatusLabel.setText(
                    "Loyalty Status: Enrollment requested"
            );
        } else {
            loyaltyStatusLabel.setText("Loyalty Status: Not enrolled");
        }

        finalTotalLabel.setText(
                String.format("Final Total: CAD %.2f", priceBreakdown.getEstimatedTotal()));
    }

    @FXML
    private void confirmReservation() {
        try {
            Guest guest = new Guest(
                    BookingSession.getFirstName(),
                    BookingSession.getLastName(),
                    BookingSession.getEmail(),
                    BookingSession.getPhone(),
                    BookingSession.getAddress()
            );

            Reservation reservation = bookingService.completeBooking(
                    guest,
                    BookingSession.getCheckInDate(),
                    BookingSession.getCheckOutDate(),
                    BookingSession.getNumAdults(),
                    BookingSession.getNumChildren(),
                    BookingSession.isGroupBooking(),
                    PaymentMethod.CARD
            );

            BookingSession.setSavedReservationId(reservation.getReservationId());
            BookingSession.setSavedGuestId(reservation.getGuest().getGuestId());
            BookingSession.setSavedRoomId(reservation.getRoom().getRoomId());

            SceneNavigator.switchTo("/views/kiosk/ConfirmationView.fxml");

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Booking Failed");
            alert.setHeaderText("Could not confirm reservation");
            alert.setContentText(getRootCauseMessage(e));
            alert.showAndWait();
        }
    }

    @FXML
    private void backToLoyaltyCheck() {
        SceneNavigator.switchTo("/views/kiosk/LoyaltyCheckView.fxml");
    }

    @FXML
    private void showRulesAndRegulations() {
        RulesDialog.show();
    }

    private String getRootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage();
    }
}
