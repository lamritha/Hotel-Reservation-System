package com.hotelreservation.util;

import com.hotelreservation.config.AppConfig;
import com.hotelreservation.model.AdminRole;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.model.WaitlistEntry;
import com.hotelreservation.model.WaitlistStatus;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.service.ReservationManagementService;
import com.hotelreservation.service.ReservationRequest;
import com.hotelreservation.service.RoomManagementService;
import com.hotelreservation.service.WaitlistRequest;
import com.hotelreservation.service.WaitlistService;

import jakarta.persistence.EntityManager;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

/**
 * One-shot verification: waitlist matching uses date overlap against the
 * room's free window (now → next CONFIRMED/CHECKED_IN check-in).
 */
public class VerifyWaitlistDateMatch {

    public static void main(String[] args) {
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
                        "VerifyWaitlistDateMatch failed: login — "
                                + login.message()
                );
                return;
            }

            LocalDate availableFrom = LocalDate.now();
            LocalDate nextBookingStart =
                    LocalDate.of(2026, 8, 20);
            LocalDate nextBookingEnd =
                    LocalDate.of(2026, 8, 22);

            LocalDate nonOverlapCheckIn =
                    LocalDate.of(2026, 9, 1);
            LocalDate nonOverlapCheckOut =
                    LocalDate.of(2026, 9, 5);

            LocalDate overlapCheckIn =
                    LocalDate.of(2026, 8, 10);
            LocalDate overlapCheckOut =
                    LocalDate.of(2026, 8, 15);

            System.out.println(
                    "=== VERIFY WAITLIST DATE MATCH ==="
            );
            System.out.println(
                    "expected_window=["
                            + availableFrom
                            + " .. "
                            + nextBookingStart
                            + ")"
            );
            System.out.println(
                    "entry_A_non_overlap="
                            + nonOverlapCheckIn
                            + ".."
                            + nonOverlapCheckOut
            );
            System.out.println(
                    "entry_B_overlap="
                            + overlapCheckIn
                            + ".."
                            + overlapCheckOut
            );

            if (!availableFrom.isBefore(nextBookingStart)) {
                System.out.println(
                        "VerifyWaitlistDateMatch failed: "
                                + "today is on/after 2026-08-20; "
                                + "adjust scenario dates."
                );
                return;
            }

            RoomRepository roomRepository = roomRepository();
            List<Room> doubles = roomRepository
                    .findAvailableRoomsByTypeAndDates(
                            RoomType.DOUBLE,
                            nextBookingStart,
                            nextBookingEnd,
                            1
                    );
            if (doubles.isEmpty()) {
                System.out.println(
                        "VerifyWaitlistDateMatch failed: "
                                + "no DOUBLE free for "
                                + nextBookingStart
                                + ".."
                                + nextBookingEnd
                );
                return;
            }

            Room targetRoom = doubles.getFirst();
            System.out.println(
                    "target_room="
                            + targetRoom.getRoomNumber()
                            + " (id="
                            + targetRoom.getRoomId()
                            + ")"
            );

            long stamp = System.currentTimeMillis();

            ReservationManagementService reservations =
                    AppConfig.getReservationManagementService();
            Reservation nextBooking =
                    reservations.createPhoneReservation(
                            new ReservationRequest(
                                    "Next",
                                    "Booking",
                                    "next.booking"
                                            + stamp
                                            + "@email.com",
                                    phone(stamp),
                                    "1 Next Booking St",
                                    nextBookingStart,
                                    nextBookingEnd,
                                    2,
                                    0,
                                    List.of(
                                            targetRoom
                                                    .getRoomId()
                                    )
                            )
                    );
            System.out.println(
                    "next_booking_reservation_id="
                            + nextBooking.getReservationId()
                            + " check_in="
                            + nextBookingStart
            );

            WaitlistService waitlistService =
                    waitlistService();

            WaitlistEntry entryA = waitlistService.add(
                    new WaitlistRequest(
                            "Wait",
                            "NonOverlap",
                            "wait.nonoverlap"
                                    + stamp
                                    + "@email.com",
                            phone(stamp + 1),
                            "1 Non Overlap St",
                            RoomType.DOUBLE,
                            nonOverlapCheckIn,
                            nonOverlapCheckOut,
                            2,
                            0
                    )
            );

            WaitlistEntry entryB = waitlistService.add(
                    new WaitlistRequest(
                            "Wait",
                            "Overlap",
                            "wait.overlap"
                                    + stamp
                                    + "@email.com",
                            phone(stamp + 2),
                            "1 Overlap St",
                            RoomType.DOUBLE,
                            overlapCheckIn,
                            overlapCheckOut,
                            2,
                            0
                    )
            );

            System.out.println(
                    "created_entry_A_id="
                            + entryA.getWaitlistId()
                            + " status="
                            + entryA.getStatus()
                            + " dates="
                            + entryA.getCheckInDate()
                            + ".."
                            + entryA.getCheckOutDate()
            );
            System.out.println(
                    "created_entry_B_id="
                            + entryB.getWaitlistId()
                            + " status="
                            + entryB.getStatus()
                            + " dates="
                            + entryB.getCheckInDate()
                            + ".."
                            + entryB.getCheckOutDate()
            );

            RoomManagementService roomManagement =
                    roomManagementService();
            // Refresh managed room entity status for flip
            Room room = roomRepository.findById(
                    targetRoom.getRoomId()
            );
            roomManagement.changeStatus(
                    room,
                    RoomStatus.MAINTENANCE
            );
            room = roomRepository.findById(
                    targetRoom.getRoomId()
            );
            roomManagement.changeStatus(
                    room,
                    RoomStatus.AVAILABLE
            );
            System.out.println(
                    "triggered_room_became_available_from="
                            + availableFrom
            );

            EntityManager entityManager =
                    JpaUtil.getEntityManager();
            try {
                WaitlistEntry reloadedA = entityManager
                        .createQuery(
                                "SELECT e FROM WaitlistEntry e "
                                        + "JOIN FETCH e.guest "
                                        + "WHERE e.waitlistId = :id",
                                WaitlistEntry.class
                        )
                        .setParameter(
                                "id",
                                entryA.getWaitlistId()
                        )
                        .getSingleResult();
                WaitlistEntry reloadedB = entityManager
                        .createQuery(
                                "SELECT e FROM WaitlistEntry e "
                                        + "JOIN FETCH e.guest "
                                        + "WHERE e.waitlistId = :id",
                                WaitlistEntry.class
                        )
                        .setParameter(
                                "id",
                                entryB.getWaitlistId()
                        )
                        .getSingleResult();

                System.out.println("--- After match ---");
                System.out.println(String.format(
                        "entry_A id=%d guest=%s dates=%s..%s status=%s",
                        reloadedA.getWaitlistId(),
                        reloadedA.getGuest().getFullName(),
                        reloadedA.getCheckInDate(),
                        reloadedA.getCheckOutDate(),
                        reloadedA.getStatus()
                ));
                System.out.println(String.format(
                        "entry_B id=%d guest=%s dates=%s..%s status=%s",
                        reloadedB.getWaitlistId(),
                        reloadedB.getGuest().getFullName(),
                        reloadedB.getCheckInDate(),
                        reloadedB.getCheckOutDate(),
                        reloadedB.getStatus()
                ));

                boolean aUnmatched =
                        reloadedA.getStatus()
                                == WaitlistStatus.WAITING;
                boolean bMatched =
                        reloadedB.getStatus()
                                == WaitlistStatus.NOTIFIED;

                System.out.println("--- Assertion ---");
                System.out.println(
                        "entry_A_still_WAITING=" + aUnmatched
                );
                System.out.println(
                        "entry_B_NOTIFIED=" + bMatched
                );
                System.out.println(
                        "PASS="
                                + (aUnmatched && bMatched)
                );
            } finally {
                if (entityManager.isOpen()) {
                    entityManager.close();
                }
            }

        } catch (Exception e) {
            System.out.println(
                    "VerifyWaitlistDateMatch failed."
            );
            e.printStackTrace();
        } finally {
            AppConfig.shutdown();
        }
    }

    private static String phone(long stamp) {
        return String.format(
                "416-555-%04d",
                Math.floorMod(stamp, 10000)
        );
    }

    @SuppressWarnings("unchecked")
    private static <T> T field(String name)
            throws Exception {
        Field field = AppConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        return (T) field.get(null);
    }

    private static WaitlistService waitlistService()
            throws Exception {
        return field("WAITLIST_SERVICE");
    }

    private static RoomManagementService
    roomManagementService() throws Exception {
        return field("ROOM_MANAGEMENT_SERVICE");
    }

    private static RoomRepository roomRepository()
            throws Exception {
        return field("ROOM_REPOSITORY");
    }
}
