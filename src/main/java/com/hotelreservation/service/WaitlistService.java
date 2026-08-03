package com.hotelreservation.service;

import com.hotelreservation.model.AdminNotification;
import com.hotelreservation.model.Guest;
import com.hotelreservation.model.NotificationType;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.WaitlistEntry;
import com.hotelreservation.model.WaitlistStatus;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.NotificationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.repository.WaitlistRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.util.AppLogger;
import com.hotelreservation.util.JpaUtil;
import com.hotelreservation.util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WaitlistService {

    private static final Logger LOGGER =
            AppLogger.getLogger(WaitlistService.class);

    private final WaitlistRepository waitlistRepository;
    private final GuestRepository guestRepository;
    private final RoomRepository roomRepository;
    private final ReservationManagementService
            reservationManagementService;
    private final NotificationRepository
            notificationRepository;
    private final AdminSession adminSession;

    public WaitlistService(
            WaitlistRepository waitlistRepository,
            GuestRepository guestRepository,
            RoomRepository roomRepository,
            ReservationManagementService
                    reservationManagementService,
            NotificationRepository notificationRepository,
            AdminSession adminSession
    ) {
        this.waitlistRepository = waitlistRepository;
        this.guestRepository = guestRepository;
        this.roomRepository = roomRepository;
        this.reservationManagementService =
                reservationManagementService;
        this.notificationRepository =
                notificationRepository;
        this.adminSession = adminSession;
    }

    public List<WaitlistEntry> search(
            String keyword,
            WaitlistStatus status
    ) {
        adminSession.requireCurrentUser();
        return waitlistRepository.search(keyword, status);
    }

    public WaitlistEntry add(WaitlistRequest request) {
        adminSession.requireCurrentUser();
        validate(request);

        WaitlistEntry saved = JpaUtil.executeInTransaction(
                () -> {
                    Guest guest = guestRepository
                            .findByEmail(request.email())
                            .map(existing -> {
                                existing.setFirstName(
                                        request.firstName().trim()
                                );
                                existing.setLastName(
                                        request.lastName().trim()
                                );
                                existing.setPhone(
                                        request.phone().trim()
                                );
                                existing.setAddress(
                                        request.address().trim()
                                );
                                return guestRepository.update(
                                        existing
                                );
                            })
                            .orElseGet(() ->
                                    guestRepository.save(
                                            new Guest(
                                                    request.firstName()
                                                    .trim(),
                                                    request.lastName()
                                                    .trim(),
                                                    request.email()
                                                    .trim()
                                                    .toLowerCase(),
                                                    request.phone()
                                                    .trim(),
                                                    request.address()
                                                    .trim()
                                            )
                                    )
                            );

                    return waitlistRepository.save(
                            new WaitlistEntry(
                                    guest,
                                    request.roomType(),
                                    request.checkInDate(),
                                    request.checkOutDate(),
                                    request.numAdults(),
                                    request.numChildren()
                            )
                    );
                }
        );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "WAITLIST_ADDED",
                "WaitlistEntry",
                String.valueOf(saved.getWaitlistId()),
                "Guest added for "
                        + request.roomType()
                        + " from " + request.checkInDate()
                        + " to " + request.checkOutDate() + "."
        );
        return saved;
    }

    public Reservation convertToReservation(
            Long waitlistId
    ) {
        adminSession.requireCurrentUser();
        WaitlistEntry entry =
                waitlistRepository.findById(waitlistId);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Waitlist entry was not found."
            );
        }
        if (entry.getStatus() != WaitlistStatus.WAITING
                && entry.getStatus()
                != WaitlistStatus.NOTIFIED) {
            throw new IllegalStateException(
                    "Only waiting or notified entries can be converted."
            );
        }

        int totalGuests =
                entry.getNumAdults() + entry.getNumChildren();
        int capacity =
                entry.getDesiredRoomType().getMaxOccupancy();
        int quantity = (int) Math.ceil(
                totalGuests / (double) capacity
        );

        List<Room> available =
                roomRepository
                        .findAvailableRoomsByTypeAndDates(
                                entry.getDesiredRoomType(),
                                entry.getCheckInDate(),
                                entry.getCheckOutDate(),
                                quantity
                        );
        if (available.size() < quantity) {
            throw new IllegalStateException(
                    "The requested room type is not available yet."
            );
        }

        Guest guest = entry.getGuest();
        Reservation reservation =
                reservationManagementService
                        .createPhoneReservation(
                                new ReservationRequest(
                                        guest.getFirstName(),
                                        guest.getLastName(),
                                        guest.getEmail(),
                                        guest.getPhone(),
                                        guest.getAddress(),
                                        entry.getCheckInDate(),
                                        entry.getCheckOutDate(),
                                        entry.getNumAdults(),
                                        entry.getNumChildren(),
                                        available.stream()
                                                .map(Room::getRoomId)
                                                .toList()
                                )
                        );

        entry.setStatus(WaitlistStatus.CONVERTED);
        entry.setConvertedReservation(reservation);
        waitlistRepository.update(entry);

        notificationRepository.save(
                new AdminNotification(
                        NotificationType.WAITLIST_MATCH,
                        adminSession.getActorName(),
                        "WAIT-" + entry.getWaitlistId()
                                + " converted to RES-"
                                + reservation.getReservationId()
                                + ".",
                        entry,
                        available.getFirst()
                )
        );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "WAITLIST_CONVERTED",
                "WaitlistEntry",
                String.valueOf(waitlistId),
                "Converted to reservation RES-"
                        + reservation.getReservationId() + "."
        );
        return reservation;
    }

    public void cancel(Long waitlistId) {
        adminSession.requireCurrentUser();
        WaitlistEntry entry =
                waitlistRepository.findById(waitlistId);
        if (entry == null) {
            throw new IllegalArgumentException(
                    "Waitlist entry was not found."
            );
        }
        if (entry.getStatus() == WaitlistStatus.CONVERTED) {
            throw new IllegalStateException(
                    "A converted waitlist entry cannot be cancelled."
            );
        }
        entry.setStatus(WaitlistStatus.CANCELLED);
        waitlistRepository.update(entry);

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "WAITLIST_CANCELLED",
                "WaitlistEntry",
                String.valueOf(waitlistId),
                "Waitlist request cancelled."
        );
    }

    private void validate(WaitlistRequest request) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Waitlist information is required."
            );
        }
        if (!ValidationUtil.isValidName(request.firstName())
                || !ValidationUtil.isValidName(
                request.lastName())) {
            throw new IllegalArgumentException(
                    "Enter valid guest first and last names."
            );
        }
        if (!ValidationUtil.isValidEmail(request.email())) {
            throw new IllegalArgumentException(
                    "Enter a valid guest email."
            );
        }
        if (!ValidationUtil.isValidPhone(request.phone())) {
            throw new IllegalArgumentException(
                    "Enter a valid guest phone number."
            );
        }
        if (!ValidationUtil.isValidAddress(request.address())) {
            throw new IllegalArgumentException(
                    "Enter a valid guest address."
            );
        }
        if (request.roomType() == null) {
            throw new IllegalArgumentException(
                    "Select a desired room type."
            );
        }
        if (request.checkInDate() == null
                || request.checkOutDate() == null
                || request.checkInDate().isBefore(LocalDate.now())
                || !request.checkOutDate()
                .isAfter(request.checkInDate())) {
            throw new IllegalArgumentException(
                    "Enter a valid future stay date range."
            );
        }
        if (request.numAdults() < 1
                || request.numChildren() < 0) {
            throw new IllegalArgumentException(
                    "At least one adult is required."
            );
        }
    }
}
