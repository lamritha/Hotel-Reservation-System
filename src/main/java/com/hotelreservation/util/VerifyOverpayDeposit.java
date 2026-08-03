package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Payment;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.PaymentType;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.service.BillingPaymentService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Pattern;

/**
 * One-shot verification: phone reservation create + oversize deposit,
 * matching the ReservationFormController Save two-step path.
 */
public class VerifyOverpayDeposit {

    public static void main(String[] args) {
        try {
            AppConfig.initialize();

            var login = AppConfig.getAuthenticationService()
                    .authenticate(
                            "admin",
                            "admin123",
                            AdminRole.ADMIN
                    );
            if (!login.successful()) {
                System.out.println(
                        "VerifyOverpayDeposit failed: login — "
                                + login.message()
                );
                return;
            }

            LocalDate checkIn = LocalDate.of(2027, 1, 10);
            LocalDate checkOut = LocalDate.of(2027, 1, 12);
            Long roomId = requireAvailableRoomId(
                    checkIn,
                    checkOut
            );
            long stamp = System.currentTimeMillis();
            String email = "verify.overpay"
                    + stamp
                    + "@email.com";
            // Keep phone valid: 416-555-XXXX using last 4 of stamp
            String phone = String.format(
                    "416-555-%04d",
                    stamp % 10000
            );

            ReservationRequest request =
                    new ReservationRequest(
                            "Verify",
                            "Overpay",
                            email,
                            phone,
                            "1 Overpay Verify St",
                            checkIn,
                            checkOut,
                            1,
                            0,
                            List.of(roomId)
                    );

            ReservationManagementService reservationService =
                    AppConfig.getReservationManagementService();
            BillingPaymentService billingService =
                    billingPaymentService();

            Reservation reservation =
                    reservationService.createPhoneReservation(
                            request
                    );
            Long reservationId =
                    reservation.getReservationId();

            System.out.println(
                    "=== VERIFY OVERPAY RESERVATION ID: "
                            + reservationId
                            + " ==="
            );

            String caughtMessage = null;
            try {
                billingService.processPayment(
                        reservationId,
                        99999.0,
                        PaymentMethod.CARD,
                        PaymentType.DEPOSIT,
                        "Deposit collected at booking"
                );
                System.out.println(
                        "UNEXPECTED: deposit succeeded"
                );
            } catch (RuntimeException exception) {
                caughtMessage = exception.getMessage();
                System.out.println(
                        "--- Caught deposit exception ---"
                );
                System.out.println(
                        "message=" + caughtMessage
                );
            }

            EntityManager entityManager =
                    JpaUtil.getEntityManager();
            try {
                Reservation persisted = entityManager
                        .createQuery(
                                "SELECT r FROM Reservation r "
                                        + "WHERE r.reservationId = :rid",
                                Reservation.class
                        )
                        .setParameter("rid", reservationId)
                        .getSingleResult();

                Billing billing = entityManager
                        .createQuery(
                                "SELECT b FROM Billing b "
                                        + "WHERE b.reservation.reservationId = :rid",
                                Billing.class
                        )
                        .setParameter("rid", reservationId)
                        .getSingleResult();

                List<Payment> payments = entityManager
                        .createQuery(
                                "SELECT p FROM Payment p "
                                        + "WHERE p.billing.billingId = :bid",
                                Payment.class
                        )
                        .setParameter(
                                "bid",
                                billing.getBillingId()
                        )
                        .getResultList();

                double outstanding =
                        billingService.calculateOutstanding(
                                billing
                        );

                System.out.println("--- Reservation ---");
                System.out.println(String.format(
                        "reservation_id=%d | status=%s | guest=%s %s",
                        persisted.getReservationId(),
                        persisted.getStatus(),
                        persisted.getGuest().getFirstName(),
                        persisted.getGuest().getLastName()
                ));

                System.out.println("--- Billing ---");
                System.out.println(String.format(
                        "billing_id=%d | total_amount=%.2f | outstanding=%.2f",
                        billing.getBillingId(),
                        billing.getTotalAmount(),
                        outstanding
                ));

                System.out.println("--- Payments ---");
                System.out.println(
                        "payment_row_count=" + payments.size()
                );
                for (Payment payment : payments) {
                    System.out.println(String.format(
                            "payment_id=%d | amount=%.2f | type=%s | method=%s | note=%s",
                            payment.getPaymentId(),
                            payment.getAmount(),
                            payment.getPaymentType(),
                            payment.getPaymentMethod(),
                            payment.getNote()
                    ));
                }

                boolean matchesExpectedPattern =
                        caughtMessage != null
                                && Pattern.compile(
                                        "^Payment exceeds the outstanding "
                                                + "balance of CAD \\d+\\.\\d{2}\\.$"
                                )
                                .matcher(caughtMessage)
                                .matches();

                System.out.println(
                        "--- UI WARNING message contract ---"
                );
                System.out.println(
                        "matches_expected_pattern="
                                + matchesExpectedPattern
                );
                if (caughtMessage != null) {
                    System.out.println(
                            "WARNING dialog would receive: "
                                    + "Reservation "
                                    + reservationId
                                    + " created successfully, but the "
                                    + "deposit could not be processed: "
                                    + caughtMessage
                                    + ". You can process a "
                                    + "payment separately from "
                                    + "Billing & Payments."
                    );
                }
            } finally {
                if (entityManager.isOpen()) {
                    entityManager.close();
                }
            }

        } catch (Exception e) {
            System.out.println(
                    "VerifyOverpayDeposit failed."
            );
            e.printStackTrace();
        } finally {
            AppConfig.shutdown();
        }
    }

    private static BillingPaymentService
    billingPaymentService() throws Exception {
        Field field = AppConfig.class.getDeclaredField(
                "BILLING_PAYMENT_SERVICE"
        );
        field.setAccessible(true);
        return (BillingPaymentService) field.get(null);
    }

    private static Long requireAvailableRoomId(
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        EntityManager entityManager =
                JpaUtil.getEntityManager();
        try {
            List<Room> available = entityManager
                    .createQuery(
                            "SELECT r FROM Room r "
                                    + "WHERE r.status <> :maintenance "
                                    + "AND NOT EXISTS ("
                                    + "  SELECT res FROM Reservation res "
                                    + "  WHERE res.room = r "
                                    + "  AND res.status IN ("
                                    + "    com.hotelreservation.model.ReservationStatus.CONFIRMED, "
                                    + "    com.hotelreservation.model.ReservationStatus.CHECKED_IN"
                                    + "  ) "
                                    + "  AND res.checkInDate < :checkOut "
                                    + "  AND res.checkOutDate > :checkIn"
                                    + ") "
                                    + "AND NOT EXISTS ("
                                    + "  SELECT rr FROM ReservationRoom rr "
                                    + "  WHERE rr.room = r "
                                    + "  AND rr.reservation.status IN ("
                                    + "    com.hotelreservation.model.ReservationStatus.CONFIRMED, "
                                    + "    com.hotelreservation.model.ReservationStatus.CHECKED_IN"
                                    + "  ) "
                                    + "  AND rr.reservation.checkInDate < :checkOut "
                                    + "  AND rr.reservation.checkOutDate > :checkIn"
                                    + ") "
                                    + "ORDER BY r.roomNumber",
                            Room.class
                    )
                    .setParameter(
                            "maintenance",
                            RoomStatus.MAINTENANCE
                    )
                    .setParameter("checkIn", checkIn)
                    .setParameter("checkOut", checkOut)
                    .getResultList();
            if (available.isEmpty()) {
                throw new IllegalStateException(
                        "No AVAILABLE room found for verify run."
                );
            }
            return available.getFirst().getRoomId();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
