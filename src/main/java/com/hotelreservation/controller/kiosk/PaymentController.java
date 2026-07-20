package com.hotelreservation.controller.kiosk;

import com.hotelreservation.entity.Guest;
import com.hotelreservation.entity.PaymentMethod;
import com.hotelreservation.entity.Reservation;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.SceneNavigator;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;

public class PaymentController {

    private final BookingService bookingService = new BookingService();

    @FXML
    private void backToBookingSummary() {
        SceneNavigator.switchTo("/views/kiosk/BookingSummaryView.fxml");
    }

    @FXML
    private void completePayment() {
        try {
            System.out.println("Attempting to save booking...");
            System.out.println("Guest: " + BookingSession.getFirstName() + " " + BookingSession.getLastName());
            System.out.println("Email: " + BookingSession.getEmail());
            System.out.println("Selected Room Type: " + BookingSession.getSelectedRoomType());
            System.out.println("Adults: " + BookingSession.getNumAdults());
            System.out.println("Children: " + BookingSession.getNumChildren());
            System.out.println("Check-in: " + BookingSession.getCheckInDate());
            System.out.println("Check-out: " + BookingSession.getCheckOutDate());

            Guest guest = new Guest(
                    BookingSession.getFirstName(),
                    BookingSession.getLastName(),
                    BookingSession.getEmail(),
                    BookingSession.getPhone(),
                    BookingSession.getAddress()
            );

            Reservation reservation = bookingService.completeBooking(
                    guest,
                    BookingSession.getSelectedRoomType(),
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

            System.out.println("Booking saved from kiosk UI.");
            System.out.println("Guest Saved: " + reservation.getGuest().getFullName());
            System.out.println("Selected Room Type: " + BookingSession.getSelectedRoomType());
            System.out.println("Room Type Saved: " + reservation.getRoom().getRoomType());
            System.out.println("Reservation ID: " + reservation.getReservationId());
            System.out.println("Guest ID: " + reservation.getGuest().getGuestId());
            System.out.println("Room ID: " + reservation.getRoom().getRoomId());
            System.out.println("Adults: " + reservation.getNumAdults());
            System.out.println("Children: " + reservation.getNumChildren());
            System.out.println("Check-in: " + reservation.getCheckInDate());
            System.out.println("Check-out: " + reservation.getCheckOutDate());
            System.out.println("Reservation Status: " + reservation.getStatus());

            SceneNavigator.switchTo("/views/kiosk/ConfirmationView.fxml");

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Booking Failed");
            alert.setHeaderText("Could not complete booking");
            alert.setContentText(getRootCauseMessage(e));
            alert.showAndWait();
        }
    }

    private String getRootCauseMessage(Throwable throwable) {
        Throwable cause = throwable;

        while (cause.getCause() != null) {
            cause = cause.getCause();
        }

        return cause.getMessage();
    }
}
