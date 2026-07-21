package com.hotelreservation.controller.kiosk;

import com.hotelreservation.entity.Guest;
import com.hotelreservation.entity.PaymentMethod;
import com.hotelreservation.entity.Reservation;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.OccupancyService;
import com.hotelreservation.service.PricingService;
import com.hotelreservation.util.BookingSession;
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
    private Label loyaltyDiscountLabel;

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

    private final OccupancyService occupancyService = new OccupancyService();
    private final PricingService pricingService = new PricingService();
    private final BookingService bookingService = new BookingService();

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

        loyaltyDiscountLabel.setText(
                String.format("Loyalty Discount: CAD %.2f", priceBreakdown.getLoyaltyDiscount()));

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
                    PaymentMethod.CARD,
                    true
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
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Rules & Regulations");
        alert.setHeaderText("Hotel Rules & Regulations");
        alert.setContentText("Placeholder: rules and regulations content will be added later.");
        alert.showAndWait();
    }

    private String getRootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage();
    }
}
