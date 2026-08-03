package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.Guest;
import com.hotelreservation.model.PaymentMethod;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.service.BookingService;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

/**
 * Verifies shared ReservationValidator on admin create and kiosk completeBooking
 * (valid success + invalid bypass rejection).
 */
public class VerifySharedReservationValidation {

    public static void main(String[] args) {
        boolean allPassed = true;

        try {
            AppConfig.initialize();

            var login = AppConfig.getAuthenticationService()
                    .authenticate(
                            "admin",
                            "ChangeMe!2026Admin",
                            AdminRole.ADMIN
                    );
            if (!login.successful()) {
                System.out.println(
                        "FAIL login: " + login.message()
                );
                return;
            }

            long stamp = System.currentTimeMillis();
            LocalDate adminCheckIn = LocalDate.of(2027, 6, 1);
            LocalDate adminCheckOut = LocalDate.of(2027, 6, 3);
            LocalDate kioskCheckIn = LocalDate.of(2027, 6, 10);
            LocalDate kioskCheckOut = LocalDate.of(2027, 6, 12);

            System.out.println("=== 1) ADMIN PHONE RESERVATION (VALID) ===");
            allPassed &= runAdminValid(stamp, adminCheckIn, adminCheckOut);

            System.out.println();
            System.out.println("=== 2) KIOSK BOOKING (VALID) ===");
            allPassed &= runKioskValid(stamp, kioskCheckIn, kioskCheckOut);

            System.out.println();
            System.out.println(
                    "=== 3) KIOSK completeBooking INVALID BYPASS ==="
            );
            allPassed &= runKioskInvalidEmail(stamp);
            allPassed &= runKioskInvalidPastCheckIn(stamp);

            System.out.println();
            System.out.println(
                    allPassed
                            ? "=== VERIFY RESULT: ALL PASSED ==="
                            : "=== VERIFY RESULT: FAILED ==="
            );
        } catch (Exception e) {
            System.out.println("VerifySharedReservationValidation crashed.");
            e.printStackTrace();
        } finally {
            JpaUtil.close();
        }
    }

    private static boolean runAdminValid(
            long stamp,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        try {
            Long roomId = requireAvailableRoomId(checkIn, checkOut);
            String email = "verify.admin.shared" + stamp + "@email.com";
            String phone = String.format("416-555-%04d", stamp % 10000);

            ReservationRequest request = new ReservationRequest(
                    "Verify",
                    "AdminShared",
                    email,
                    phone,
                    "1 Admin Shared St",
                    checkIn,
                    checkOut,
                    1,
                    0,
                    List.of(roomId)
            );

            ReservationManagementService reservations =
                    AppConfig.getReservationManagementService();
            Reservation created =
                    reservations.createPhoneReservation(request);

            System.out.println(
                    "created_reservation_id="
                            + created.getReservationId()
                            + " status="
                            + created.getStatus()
                            + " email="
                            + created.getGuest().getEmail()
            );

            boolean ok = created.getReservationId() != null
                    && created.getGuest() != null
                    && email.equalsIgnoreCase(
                            created.getGuest().getEmail()
                    );
            System.out.println(ok ? "PASS admin valid create" : "FAIL admin valid create");
            return ok;
        } catch (Exception e) {
            System.out.println("FAIL admin valid create: " + rootMessage(e));
            e.printStackTrace();
            return false;
        }
    }

    private static boolean runKioskValid(
            long stamp,
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        try {
            ensureAvailableSingleRoom();

            BookingSession.reset();
            BookingSession.setFirstName("Verify");
            BookingSession.setLastName("KioskShared");
            BookingSession.setEmail(
                    "verify.kiosk.shared" + stamp + "@email.com"
            );
            BookingSession.setPhone(
                    String.format("416-555-%04d", (stamp / 10) % 10000)
            );
            BookingSession.setAddress("2 Kiosk Shared St");
            BookingSession.setNumAdults(1);
            BookingSession.setNumChildren(0);
            BookingSession.setCheckInDate(checkIn);
            BookingSession.setCheckOutDate(checkOut);
            BookingSession.setSingleRoomQuantity(1);

            Guest guest = new Guest(
                    BookingSession.getFirstName(),
                    BookingSession.getLastName(),
                    BookingSession.getEmail(),
                    BookingSession.getPhone(),
                    BookingSession.getAddress()
            );

            long countBefore = countReservations();
            BookingService bookingService = new BookingService();
            Reservation reservation = bookingService.completeBooking(
                    guest,
                    checkIn,
                    checkOut,
                    1,
                    0,
                    false,
                    PaymentMethod.CARD
            );
            long countAfter = countReservations();

            System.out.println(
                    "created_reservation_id="
                            + reservation.getReservationId()
                            + " status="
                            + reservation.getStatus()
                            + " count_before="
                            + countBefore
                            + " count_after="
                            + countAfter
            );

            boolean ok = reservation.getReservationId() != null
                    && countAfter == countBefore + 1;
            System.out.println(
                    ok ? "PASS kiosk valid booking" : "FAIL kiosk valid booking"
            );
            return ok;
        } catch (Exception e) {
            System.out.println("FAIL kiosk valid booking: " + rootMessage(e));
            e.printStackTrace();
            return false;
        } finally {
            BookingSession.reset();
        }
    }

    private static boolean runKioskInvalidEmail(long stamp) {
        System.out.println("--- invalid email format ---");
        try {
            BookingSession.reset();
            BookingSession.setSingleRoomQuantity(1);
            BookingSession.setNumAdults(1);
            BookingSession.setNumChildren(0);

            Guest guest = new Guest(
                    "Verify",
                    "BadEmail",
                    "not-an-email",
                    String.format("416-555-%04d", (stamp / 100) % 10000),
                    "3 Bad Email St"
            );

            long countBefore = countReservations();
            String message = attemptBookingExpectingFailure(
                    guest,
                    LocalDate.of(2027, 7, 1),
                    LocalDate.of(2027, 7, 3),
                    1,
                    0
            );
            long countAfter = countReservations();

            boolean rejected =
                    message != null
                            && message.contains(
                                    "Please enter a valid email address."
                            );
            boolean noPersist = countAfter == countBefore;

            System.out.println("root_message=" + message);
            System.out.println(
                    "count_before=" + countBefore
                            + " count_after=" + countAfter
            );
            boolean ok = rejected && noPersist;
            System.out.println(
                    ok
                            ? "PASS invalid email rejected, not persisted"
                            : "FAIL invalid email case"
            );
            return ok;
        } catch (Exception e) {
            System.out.println("FAIL invalid email case: " + rootMessage(e));
            e.printStackTrace();
            return false;
        } finally {
            BookingSession.reset();
        }
    }

    private static boolean runKioskInvalidPastCheckIn(long stamp) {
        System.out.println("--- check-in date in the past ---");
        try {
            BookingSession.reset();
            BookingSession.setSingleRoomQuantity(1);
            BookingSession.setNumAdults(1);
            BookingSession.setNumChildren(0);

            Guest guest = new Guest(
                    "Verify",
                    "PastDate",
                    "verify.past" + stamp + "@email.com",
                    String.format("416-555-%04d", (stamp / 1000) % 10000),
                    "4 Past Date St"
            );

            long countBefore = countReservations();
            String message = attemptBookingExpectingFailure(
                    guest,
                    LocalDate.now().minusDays(2),
                    LocalDate.now().plusDays(1),
                    1,
                    0
            );
            long countAfter = countReservations();

            boolean rejected =
                    message != null
                            && message.contains(
                                    "Check-in date cannot be before today."
                            );
            boolean noPersist = countAfter == countBefore;

            System.out.println("root_message=" + message);
            System.out.println(
                    "count_before=" + countBefore
                            + " count_after=" + countAfter
            );
            boolean ok = rejected && noPersist;
            System.out.println(
                    ok
                            ? "PASS past check-in rejected, not persisted"
                            : "FAIL past check-in case"
            );
            return ok;
        } catch (Exception e) {
            System.out.println("FAIL past check-in case: " + rootMessage(e));
            e.printStackTrace();
            return false;
        } finally {
            BookingSession.reset();
        }
    }

    private static String attemptBookingExpectingFailure(
            Guest guest,
            LocalDate checkIn,
            LocalDate checkOut,
            int adults,
            int children
    ) {
        try {
            new BookingService().completeBooking(
                    guest,
                    checkIn,
                    checkOut,
                    adults,
                    children,
                    false,
                    PaymentMethod.CARD
            );
            System.out.println("UNEXPECTED: booking succeeded");
            return null;
        } catch (RuntimeException e) {
            return rootMessage(e);
        }
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    private static long countReservations() {
        EntityManager entityManager = JpaUtil.getEntityManager();
        try {
            return entityManager
                    .createQuery(
                            "SELECT COUNT(r) FROM Reservation r",
                            Long.class
                    )
                    .getSingleResult();
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private static void ensureAvailableSingleRoom() {
        EntityManager entityManager = JpaUtil.getEntityManager();
        try {
            List<Room> available = entityManager.createQuery(
                            "SELECT r FROM Room r "
                                    + "WHERE r.roomType = com.hotelreservation.model.RoomType.SINGLE "
                                    + "AND r.status = :s",
                            Room.class
                    )
                    .setParameter("s", RoomStatus.AVAILABLE)
                    .getResultList();
            if (!available.isEmpty()) {
                return;
            }
            throw new IllegalStateException(
                    "No AVAILABLE SINGLE room for kiosk verify."
            );
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private static Long requireAvailableRoomId(
            LocalDate checkIn,
            LocalDate checkOut
    ) {
        EntityManager entityManager = JpaUtil.getEntityManager();
        try {
            List<Room> available = entityManager.createQuery(
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
                                    + ")",
                            Room.class
                    )
                    .setParameter(
                            "maintenance",
                            RoomStatus.MAINTENANCE
                    )
                    .setParameter("checkIn", checkIn)
                    .setParameter("checkOut", checkOut)
                    .setMaxResults(1)
                    .getResultList();

            if (available.isEmpty()) {
                throw new IllegalStateException(
                        "No available room for admin verify dates."
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
