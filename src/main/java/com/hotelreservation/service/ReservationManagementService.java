package com.hotelreservation.service;

import com.hotelreservation.model.Billing;
import com.hotelreservation.model.Guest;
import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationRoom;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.util.AppLogger;
import com.hotelreservation.util.JpaUtil;
import com.hotelreservation.util.ReservationValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReservationManagementService {

    private static final Logger LOGGER =
            AppLogger.getLogger(
                    ReservationManagementService.class
            );

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final GuestRepository guestRepository;
    private final BillingRepository billingRepository;
    private final PricingService pricingService;
    private final AdminSession adminSession;

    public ReservationManagementService(
            ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            GuestRepository guestRepository,
            BillingRepository billingRepository,
            PricingService pricingService,
            AdminSession adminSession
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.guestRepository = guestRepository;
        this.billingRepository = billingRepository;
        this.pricingService = pricingService;
        this.adminSession = adminSession;
    }

    public List<Reservation> searchReservations(
            String keyword,
            ReservationStatus status,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (dateFrom != null
                && dateTo != null
                && dateTo.isBefore(dateFrom)) {

            throw new IllegalArgumentException(
                    "The ending date cannot be before the starting date."
            );
        }

        try {
            List<Reservation> reservations =
                    reservationRepository.search(
                            keyword,
                            status,
                            dateFrom,
                            dateTo
                    );

            AppLogger.audit(
                    LOGGER,
                    Level.INFO,
                    adminSession.getActorName(),
                    "RESERVATION_SEARCH",
                    "Reservation",
                    "-",
                    "Search returned "
                            + reservations.size()
                            + " reservation(s)."
            );

            return reservations;

        } catch (RuntimeException exception) {
            AppLogger.exception(
                    LOGGER,
                    "Reservation search failed.",
                    exception
            );

            throw exception;
        }
    }

    public Optional<Reservation> findReservation(
            Long reservationId
    ) {
        Optional<Reservation> reservation =
                reservationRepository.findByIdWithDetails(
                        reservationId
                );

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "RESERVATION_VIEW",
                "Reservation",
                String.valueOf(reservationId),
                reservation.isPresent()
                        ? "Reservation details opened."
                        : "Reservation was not found."
        );

        return reservation;
    }

    /**
     * Used by the administrative reservation form. Past dates are allowed
     * while loading an existing record, but saving still enforces today's
     * minimum check-in date.
     */
    public List<Room> findAvailableRooms(
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Long excludedReservationId
    ) {
        validateDateOrder(checkInDate, checkOutDate);

        return roomRepository.findAvailableRoomsForDates(
                checkInDate,
                checkOutDate,
                excludedReservationId
        );
    }

    public Reservation createPhoneReservation(
            ReservationRequest request
    ) {
        try {
            validateReservationRequest(request);

            Reservation createdReservation =
                    JpaUtil.executeInTransaction(() -> {
                        List<Room> selectedRooms =
                                resolveAvailableRooms(
                                        request.roomIds(),
                                        request.checkInDate(),
                                        request.checkOutDate(),
                                        null
                                );

                        List<RoomAllocation> allocations =
                                buildRoomAllocations(
                                        selectedRooms,
                                        request.numAdults(),
                                        request.numChildren()
                                );

                        Guest guest = resolveGuestForCreate(request);

                        Reservation reservation =
                                new Reservation(
                                        guest,
                                        selectedRooms.getFirst(),
                                        request.checkInDate(),
                                        request.checkOutDate(),
                                        request.numAdults(),
                                        request.numChildren(),
                                        selectedRooms.size() > 1
                                );

                        reservation.setStatus(
                                ReservationStatus.CONFIRMED
                        );

                        synchronizeRoomAssignments(
                                reservation,
                                allocations
                        );

                        Reservation savedReservation =
                                reservationRepository.save(
                                        reservation
                                );

                        recalculateBilling(
                                savedReservation,
                                selectedRooms
                        );

                        return savedReservation;
                    });

            AppLogger.audit(
                    LOGGER,
                    Level.INFO,
                    adminSession.getActorName(),
                    "PHONE_RESERVATION_CREATED",
                    "Reservation",
                    String.valueOf(
                            createdReservation.getReservationId()
                    ),
                    "Phone reservation created for "
                            + normalize(request.firstName())
                            + " "
                            + normalize(request.lastName())
                            + " with "
                            + request.roomIds().size()
                            + " room(s)."
            );

            return createdReservation;

        } catch (RuntimeException exception) {
            AppLogger.exception(
                    LOGGER,
                    "Phone reservation creation failed.",
                    exception
            );

            throw exception;
        }
    }

    public Reservation modifyReservation(
            Long reservationId,
            ReservationRequest request
    ) {
        try {
            if (reservationId == null) {
                throw new IllegalArgumentException(
                        "A reservation must be selected."
                );
            }

            validateReservationRequest(request);

            Reservation updatedReservation =
                    JpaUtil.executeInTransaction(() -> {
                        Reservation reservation =
                                reservationRepository
                                        .findByIdWithDetails(
                                                reservationId
                                        )
                                        .orElseThrow(() ->
                                                new IllegalArgumentException(
                                                        "Reservation was not found."
                                                )
                                        );

                        validateModifiableStatus(
                                reservation.getStatus()
                        );

                        List<Room> selectedRooms =
                                resolveAvailableRooms(
                                        request.roomIds(),
                                        request.checkInDate(),
                                        request.checkOutDate(),
                                        reservationId
                                );

                        List<RoomAllocation> allocations =
                                buildRoomAllocations(
                                        selectedRooms,
                                        request.numAdults(),
                                        request.numChildren()
                                );

                        updateReservationGuest(
                                reservation,
                                request
                        );

                        reservation.setCheckInDate(
                                request.checkInDate()
                        );
                        reservation.setCheckOutDate(
                                request.checkOutDate()
                        );
                        reservation.setNumAdults(
                                request.numAdults()
                        );
                        reservation.setNumChildren(
                                request.numChildren()
                        );
                        reservation.setRoom(
                                selectedRooms.getFirst()
                        );
                        reservation.setGroupBooking(
                                selectedRooms.size() > 1
                        );

                        synchronizeRoomAssignments(
                                reservation,
                                allocations
                        );

                        Reservation savedReservation =
                                reservationRepository.update(
                                        reservation
                                );

                        recalculateBilling(
                                savedReservation,
                                selectedRooms
                        );

                        return savedReservation;
                    });

            AppLogger.audit(
                    LOGGER,
                    Level.INFO,
                    adminSession.getActorName(),
                    "RESERVATION_MODIFIED",
                    "Reservation",
                    String.valueOf(reservationId),
                    "Reservation dates, occupancy, guest details, "
                            + "and room assignment were updated."
            );

            return updatedReservation;

        } catch (RuntimeException exception) {
            AppLogger.exception(
                    LOGGER,
                    "Reservation modification failed.",
                    exception
            );

            throw exception;
        }
    }

    public Reservation cancelReservation(Long reservationId) {
        if (reservationId == null) {
            throw new IllegalArgumentException(
                    "A reservation must be selected."
            );
        }

        try {
            Reservation reservation =
                    reservationRepository
                            .findByIdWithDetails(reservationId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Reservation was not found."
                                    )
                            );

            ReservationStatus currentStatus =
                    reservation.getStatus();

            if (currentStatus == ReservationStatus.CANCELLED) {
                throw new IllegalStateException(
                        "Reservation is already cancelled."
                );
            }

            if (currentStatus == ReservationStatus.CHECKED_IN) {
                throw new IllegalStateException(
                        "A checked-in reservation cannot be cancelled."
                );
            }

            if (currentStatus == ReservationStatus.CHECKED_OUT) {
                throw new IllegalStateException(
                        "A checked-out reservation cannot be cancelled."
                );
            }

            reservation.setStatus(
                    ReservationStatus.CANCELLED
            );

            Reservation updatedReservation =
                    reservationRepository.update(reservation);

            AppLogger.audit(
                    LOGGER,
                    Level.INFO,
                    adminSession.getActorName(),
                    "RESERVATION_CANCELLED",
                    "Reservation",
                    String.valueOf(reservationId),
                    "Reservation status changed from "
                            + currentStatus
                            + " to CANCELLED."
            );

            return updatedReservation;

        } catch (RuntimeException exception) {
            AppLogger.exception(
                    LOGGER,
                    "Reservation cancellation failed.",
                    exception
            );

            throw exception;
        }
    }

    private void validateReservationRequest(
            ReservationRequest request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Reservation information is required."
            );
        }

        ReservationValidator.validate(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.address(),
                request.checkInDate(),
                request.checkOutDate(),
                request.numAdults(),
                request.numChildren(),
                !request.roomIds().isEmpty()
        );
    }

    private void validateDateOrder(
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        ReservationValidator.validateDateOrder(checkInDate, checkOutDate);
    }

    private void validateModifiableStatus(
            ReservationStatus status
    ) {
        if (status != ReservationStatus.PENDING
                && status != ReservationStatus.CONFIRMED) {

            throw new IllegalStateException(
                    "Only pending or confirmed reservations can be modified."
            );
        }
    }

    private List<Room> resolveAvailableRooms(
            List<Long> requestedRoomIds,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            Long excludedReservationId
    ) {
        Set<Long> uniqueRoomIds =
                new LinkedHashSet<>(requestedRoomIds);

        if (uniqueRoomIds.contains(null)) {
            throw new IllegalArgumentException(
                    "A selected room is invalid."
            );
        }

        List<Room> availableRooms =
                roomRepository.findAvailableRoomsForDates(
                        checkInDate,
                        checkOutDate,
                        excludedReservationId
                );

        Map<Long, Room> availableById =
                new LinkedHashMap<>();

        for (Room room : availableRooms) {
            availableById.put(room.getRoomId(), room);
        }

        List<Room> selectedRooms = new ArrayList<>();

        for (Long roomId : uniqueRoomIds) {
            Room room = availableById.get(roomId);

            if (room == null) {
                throw new IllegalStateException(
                        "One or more selected rooms are no longer "
                                + "available. Refresh room availability "
                                + "and try again."
                );
            }

            selectedRooms.add(room);
        }

        return selectedRooms;
    }

    private List<RoomAllocation> buildRoomAllocations(
            List<Room> selectedRooms,
            int numAdults,
            int numChildren
    ) {
        int totalGuests = numAdults + numChildren;

        if (selectedRooms.size() > totalGuests) {
            throw new IllegalArgumentException(
                    "Each selected room must have at least one guest."
            );
        }

        int totalCapacity = selectedRooms.stream()
                .mapToInt(Room::getMaxOccupancy)
                .sum();

        if (totalGuests > totalCapacity) {
            throw new IllegalArgumentException(
                    "The selected rooms allow "
                            + totalCapacity
                            + " guest(s), but "
                            + totalGuests
                            + " were entered."
            );
        }

        int[] adultsByRoom =
                new int[selectedRooms.size()];
        int[] childrenByRoom =
                new int[selectedRooms.size()];

        int remainingAdults = numAdults;
        int remainingChildren = numChildren;

        // Give every selected room one guest before filling remaining space.
        for (int index = 0;
             index < selectedRooms.size();
             index++) {

            if (remainingAdults > 0) {
                adultsByRoom[index]++;
                remainingAdults--;
            } else {
                childrenByRoom[index]++;
                remainingChildren--;
            }
        }

        for (int index = 0;
             index < selectedRooms.size();
             index++) {

            int roomCapacity =
                    selectedRooms.get(index)
                            .getMaxOccupancy();

            int assignedGuests =
                    adultsByRoom[index]
                            + childrenByRoom[index];

            int remainingCapacity =
                    roomCapacity - assignedGuests;

            int adultsToAssign =
                    Math.min(
                            remainingAdults,
                            remainingCapacity
                    );

            adultsByRoom[index] += adultsToAssign;
            remainingAdults -= adultsToAssign;
            remainingCapacity -= adultsToAssign;

            int childrenToAssign =
                    Math.min(
                            remainingChildren,
                            remainingCapacity
                    );

            childrenByRoom[index] += childrenToAssign;
            remainingChildren -= childrenToAssign;
        }

        if (remainingAdults > 0 || remainingChildren > 0) {
            throw new IllegalStateException(
                    "The guests could not be distributed "
                            + "across the selected rooms."
            );
        }

        List<RoomAllocation> allocations =
                new ArrayList<>();

        for (int index = 0;
             index < selectedRooms.size();
             index++) {

            allocations.add(
                    new RoomAllocation(
                            selectedRooms.get(index),
                            adultsByRoom[index],
                            childrenByRoom[index]
                    )
            );
        }

        return allocations;
    }

    private Guest resolveGuestForCreate(
            ReservationRequest request
    ) {
        Optional<Guest> existingGuest =
                guestRepository.findByEmail(
                        normalize(request.email())
                );

        if (existingGuest.isPresent()) {
            Guest guest = existingGuest.get();
            applyGuestDetails(guest, request);
            return guestRepository.update(guest);
        }

        Guest guest = new Guest(
                normalize(request.firstName()),
                normalize(request.lastName()),
                normalize(request.email()),
                normalize(request.phone()),
                normalize(request.address())
        );

        return guestRepository.save(guest);
    }

    private void updateReservationGuest(
            Reservation reservation,
            ReservationRequest request
    ) {
        Guest guest = reservation.getGuest();

        Optional<Guest> emailOwner =
                guestRepository.findByEmail(
                        normalize(request.email())
                );

        if (emailOwner.isPresent()
                && !Objects.equals(
                emailOwner.get().getGuestId(),
                guest.getGuestId()
        )) {

            throw new IllegalArgumentException(
                    "That email address belongs to another guest."
            );
        }

        applyGuestDetails(guest, request);
        guestRepository.update(guest);
    }

    private void applyGuestDetails(
            Guest guest,
            ReservationRequest request
    ) {
        guest.setFirstName(
                normalize(request.firstName())
        );
        guest.setLastName(
                normalize(request.lastName())
        );
        guest.setEmail(
                normalize(request.email())
        );
        guest.setPhone(
                normalize(request.phone())
        );
        guest.setAddress(
                normalize(request.address())
        );
    }

    private void synchronizeRoomAssignments(
            Reservation reservation,
            List<RoomAllocation> allocations
    ) {
        Set<Long> selectedRoomIds =
                new LinkedHashSet<>();

        for (RoomAllocation allocation : allocations) {
            selectedRoomIds.add(
                    allocation.room().getRoomId()
            );
        }

        reservation.getReservationRooms().removeIf(
                reservationRoom ->
                        reservationRoom.getRoom() == null
                                || !selectedRoomIds.contains(
                                reservationRoom
                                        .getRoom()
                                        .getRoomId()
                        )
        );

        Map<Long, ReservationRoom> existingByRoomId =
                new LinkedHashMap<>();

        for (ReservationRoom reservationRoom
                : reservation.getReservationRooms()) {

            existingByRoomId.put(
                    reservationRoom
                            .getRoom()
                            .getRoomId(),
                    reservationRoom
            );
        }

        for (RoomAllocation allocation : allocations) {
            Long roomId =
                    allocation.room().getRoomId();

            ReservationRoom reservationRoom =
                    existingByRoomId.get(roomId);

            if (reservationRoom == null) {
                reservation.addReservationRoom(
                        new ReservationRoom(
                                reservation,
                                allocation.room(),
                                allocation.assignedAdults(),
                                allocation.assignedChildren()
                        )
                );
            } else {
                reservationRoom.setAssignedAdults(
                        allocation.assignedAdults()
                );
                reservationRoom.setAssignedChildren(
                        allocation.assignedChildren()
                );
            }
        }
    }

    private void recalculateBilling(
            Reservation reservation,
            List<Room> selectedRooms
    ) {
        double nightlyRoomCost =
                selectedRooms.stream()
                        .mapToDouble(Room::getBasePrice)
                        .sum();

        double roomTotal =
                pricingService.calculateRoomSubtotal(
                        nightlyRoomCost,
                        reservation.getCheckInDate(),
                        reservation.getCheckOutDate(),
                        true
                );

        Billing billingProbe =
                new Billing(reservation, 0, 0, 0);

        double subtotal = roundMoney(
                roomTotal
                        + billingProbe
                        .getPersistedAddOnTotal()
        );

        double taxAmount = roundMoney(
                pricingService.calculateTax(subtotal)
        );

        double totalAmount = roundMoney(
                pricingService.calculateEstimatedTotal(
                        subtotal,
                        taxAmount
                )
        );

        Optional<Billing> existingBilling =
                billingRepository.findByReservationId(
                        reservation.getReservationId()
                );

        if (existingBilling.isPresent()) {
            Billing billing = existingBilling.get();
            billing.setSubtotal(subtotal);
            billing.setTaxAmount(taxAmount);
            billing.setTotalAmount(totalAmount);
            billingRepository.update(billing);
            return;
        }

        billingRepository.save(
                new Billing(
                        reservation,
                        subtotal,
                        taxAmount,
                        totalAmount
                )
        );
    }

    private double roundMoney(double amount) {
        return Math.round(amount * 100.0) / 100.0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record RoomAllocation(
            Room room,
            int assignedAdults,
            int assignedChildren
    ) {
    }
}
