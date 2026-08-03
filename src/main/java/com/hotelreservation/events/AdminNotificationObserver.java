package com.hotelreservation.events;

import com.hotelreservation.model.AdminNotification;
import com.hotelreservation.model.NotificationType;
import com.hotelreservation.model.WaitlistEntry;
import com.hotelreservation.model.WaitlistStatus;
import com.hotelreservation.repository.NotificationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.WaitlistRepository;
import com.hotelreservation.util.AppLogger;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer that converts room-availability events into persisted admin
 * notifications for matching waitlist entries.
 */
public class AdminNotificationObserver
        implements RoomAvailabilityObserver {

    private static final Logger LOGGER =
            AppLogger.getLogger(
                    AdminNotificationObserver.class
            );

    private final WaitlistRepository waitlistRepository;
    private final NotificationRepository
            notificationRepository;
    private final RoomRepository roomRepository;

    public AdminNotificationObserver(
            WaitlistRepository waitlistRepository,
            NotificationRepository notificationRepository,
            RoomRepository roomRepository
    ) {
        this.waitlistRepository = waitlistRepository;
        this.notificationRepository =
                notificationRepository;
        this.roomRepository = roomRepository;
    }

    @Override
    public void onRoomAvailable(
            RoomAvailabilityEvent event
    ) {
        LocalDate availableFrom = event.availableFrom();
        LocalDate availableTo = roomRepository
                .findNextReservationStart(
                        event.room(),
                        availableFrom
                )
                .orElseGet(
                        () -> availableFrom.plusYears(10)
                );

        List<WaitlistEntry> matches =
                waitlistRepository
                        .findWaitingByRoomTypeAndDates(
                                event.room().getRoomType(),
                                availableFrom,
                                availableTo
                        );

        for (WaitlistEntry entry : matches) {
            if (entry.getStatus() != WaitlistStatus.WAITING) {
                continue;
            }

            entry.setStatus(WaitlistStatus.NOTIFIED);
            waitlistRepository.update(entry);

            String message = "Room "
                    + event.room().getRoomNumber()
                    + " (" + event.room().getRoomType()
                    + ") is available for waitlist WAIT-"
                    + entry.getWaitlistId()
                    + " belonging to "
                    + entry.getGuest().getFullName() + ".";

            // Broadcast to all admins (recipient "ALL"): deliberate simplification —
            // there is no per-admin subscribe/unsubscribe UI yet. Known Final scope
            // limitation, not an oversight; every logged-in admin sees these.
            notificationRepository.save(
                    new AdminNotification(
                            NotificationType.WAITLIST_MATCH,
                            "ALL",
                            message,
                            entry,
                            event.room()
                    )
            );

            AppLogger.audit(
                    LOGGER,
                    Level.INFO,
                    "SYSTEM",
                    "WAITLIST_AVAILABILITY_NOTIFICATION",
                    "WaitlistEntry",
                    String.valueOf(entry.getWaitlistId()),
                    message
            );
        }

        if (matches.isEmpty()) {
            // Same "ALL" broadcast as waitlist matches (no subscription model yet).
            notificationRepository.save(
                    new AdminNotification(
                            NotificationType.ROOM_AVAILABLE,
                            "ALL",
                            "Room "
                                    + event.room().getRoomNumber()
                                    + " is now available.",
                            null,
                            event.room()
                    )
            );
        }
    }
}
