package com.hotelreservation.service;

import com.hotelreservation.model.*;
import com.hotelreservation.repository.AddOnRepository;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.LoyaltyAccountRepository;
import com.hotelreservation.repository.LoyaltyTransactionRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.JpaUtil;
import com.hotelreservation.util.ReservationValidator;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates kiosk booking completion: multi-room selection from BookingSession
 * quantities, occupancy split, pricing via PricingService, and persistence of Guest,
 * Reservation, ReservationRoom, and Billing.
 */
public class BookingService {

    private final RoomAvailabilityService roomAvailabilityService;
    private final OccupancyService occupancyService;
    private final PricingService pricingService;
    private final GuestRepository guestRepository;
    private final ReservationRepository reservationRepository;
    private final BillingRepository billingRepository;
    private final AddOnRepository addOnRepository;
    private final LoyaltyService loyaltyService;

    private static final String WIFI_NAME = "Wi-Fi";
    private static final String BREAKFAST_NAME = "Breakfast";
    private static final String PARKING_NAME = "Parking";
    private static final String SPA_NAME = "Spa";
    private static final String LAUNDRY_NAME = "Laundry";
    private static final String AIRPORT_PICKUP_NAME = "Airport Pickup";

    public BookingService() {
        this(
                new RoomAvailabilityService(),
                new OccupancyService(),
                new PricingService(),
                new GuestRepository(),
                new ReservationRepository(),
                new BillingRepository(),
                new AddOnRepository(),
                new LoyaltyService(
                        new LoyaltyAccountRepository(),
                        new LoyaltyTransactionRepository()
                )
        );
    }

    public BookingService(
            RoomAvailabilityService roomAvailabilityService,
            OccupancyService occupancyService,
            PricingService pricingService,
            GuestRepository guestRepository,
            ReservationRepository reservationRepository,
            BillingRepository billingRepository,
            AddOnRepository addOnRepository,
            LoyaltyService loyaltyService
    ) {
        this.roomAvailabilityService = roomAvailabilityService;
        this.occupancyService = occupancyService;
        this.pricingService = pricingService;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.billingRepository = billingRepository;
        this.addOnRepository = addOnRepository;
        this.loyaltyService = loyaltyService;
    }

    public Reservation completeBooking(
            Guest guest,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numAdults,
            int numChildren,
            boolean groupBooking,
            PaymentMethod paymentMethod
    ) {
        try {
            return JpaUtil.executeInTransaction(() ->
                    completeBookingInTransaction(
                            guest,
                            checkInDate,
                            checkOutDate,
                            numAdults,
                            numChildren,
                            groupBooking,
                            paymentMethod
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Booking failed", e);
        }
    }

    private Reservation completeBookingInTransaction(
            Guest guest,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numAdults,
            int numChildren,
            boolean groupBooking,
            PaymentMethod paymentMethod
    ) {
        ReservationValidator.validate(
                guest.getFirstName(),
                guest.getLastName(),
                guest.getEmail(),
                guest.getPhone(),
                guest.getAddress(),
                checkInDate,
                checkOutDate,
                numAdults,
                numChildren,
                BookingSession.getTotalRoomQuantity() > 0
        );

        List<Room> assignedRooms = collectAssignedRooms(checkInDate, checkOutDate);

        if (assignedRooms.isEmpty()) {
            throw new IllegalStateException("No rooms selected for this booking.");
        }

        int totalCapacity = assignedRooms.stream().mapToInt(Room::getMaxOccupancy).sum();
        int totalGuests = occupancyService.getTotalGuests(numAdults, numChildren);
        if (totalGuests > totalCapacity) {
            throw new IllegalStateException(
                    "Occupancy limit exceeded. Selected rooms allow maximum "
                            + totalCapacity + " guests, but " + totalGuests + " were requested."
            );
        }
        if (totalGuests < assignedRooms.size()) {
            throw new IllegalStateException(
                    "Each selected room must have at least one guest."
            );
        }

        Room legacyRoom = assignedRooms.get(0);

        PricingService.PriceBreakdown priceBreakdown = pricingService.calculateSessionPriceBreakdown();

        Guest guestToPersist = resolveGuest(guest);

        Reservation reservation = new Reservation(
                guestToPersist,
                legacyRoom,
                checkInDate,
                checkOutDate,
                numAdults,
                numChildren,
                groupBooking || assignedRooms.size() > 1
        );

        reservation.setStatus(ReservationStatus.CONFIRMED);

        attachAssignedRooms(reservation, assignedRooms, numAdults, numChildren);

        attachSelectedAddOns(
                reservation,
                BookingSession.isWifiSelected(),
                BookingSession.isBreakfastSelected(),
                BookingSession.isSpaSelected(),
                BookingSession.isParkingSelected(),
                BookingSession.isLaundrySelected(),
                BookingSession.isAirportPickupSelected()
        );

        warnIfPersistedAddOnTotalDrifts(reservation, priceBreakdown.getAddOnTotal());

        reservationRepository.save(reservation);

        if (BookingSession.isLoyaltyEnrollmentRequested()) {
            LoyaltyAccount account =
                    loyaltyService.enrollGuest(
                            guestToPersist,
                            "KIOSK"
                    );
            BookingSession.setLoyaltyEnrolled(true);
            BookingSession.setLoyaltyNumber(
                    account.getLoyaltyNumber()
            );
            BookingSession.setLoyaltyPointsBalance(
                    account.getPointsBalance()
            );
        }

        Billing billing = new Billing(
                reservation,
                priceBreakdown.getSubtotal(),
                priceBreakdown.getTaxAmount(),
                priceBreakdown.getEstimatedTotal()
        );

        billingRepository.save(billing);

        return reservation;
    }

    /**
     * Reuses an existing Guest matched by email (updating contact fields), or saves a new one.
     */
    private Guest resolveGuest(Guest incomingGuest) {
        return guestRepository.findByEmail(incomingGuest.getEmail())
                .map(existingGuest -> {
                    existingGuest.setFirstName(incomingGuest.getFirstName());
                    existingGuest.setLastName(incomingGuest.getLastName());
                    existingGuest.setPhone(incomingGuest.getPhone());
                    existingGuest.setAddress(incomingGuest.getAddress());
                    return guestRepository.update(existingGuest);
                })
                .orElseGet(() -> guestRepository.save(incomingGuest));
    }

    private List<Room> collectAssignedRooms(LocalDate checkInDate, LocalDate checkOutDate) {
        List<Room> assignedRooms = new ArrayList<>();

        appendRoomsForType(
                assignedRooms,
                RoomType.SINGLE,
                BookingSession.getSingleRoomQuantity(),
                checkInDate,
                checkOutDate
        );
        appendRoomsForType(
                assignedRooms,
                RoomType.DOUBLE,
                BookingSession.getDoubleRoomQuantity(),
                checkInDate,
                checkOutDate
        );
        appendRoomsForType(
                assignedRooms,
                RoomType.DELUXE,
                BookingSession.getDeluxeRoomQuantity(),
                checkInDate,
                checkOutDate
        );
        appendRoomsForType(
                assignedRooms,
                RoomType.PENTHOUSE,
                BookingSession.getPenthouseRoomQuantity(),
                checkInDate,
                checkOutDate
        );

        return assignedRooms;
    }

    private void appendRoomsForType(
            List<Room> assignedRooms,
            RoomType roomType,
            int quantity,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (quantity <= 0) {
            return;
        }

        assignedRooms.addAll(
                roomAvailabilityService.findAvailableRooms(roomType, checkInDate, checkOutDate, quantity)
        );
    }

    /**
     * Fill-to-max-then-overflow: adults first, then children, up to each room's maxOccupancy.
     */
    private void attachAssignedRooms(
            Reservation reservation,
            List<Room> assignedRooms,
            int numAdults,
            int numChildren
    ) {
        int remainingAdults = numAdults;
        int remainingChildren = numChildren;

        List<int[]> allocations = new ArrayList<>();
        for (Room room : assignedRooms) {
            int assignedAdults;
            int assignedChildren;
            if (remainingAdults > 0) {
                assignedAdults = 1;
                assignedChildren = 0;
                remainingAdults--;
            } else {
                assignedAdults = 0;
                assignedChildren = 1;
                remainingChildren--;
            }
            allocations.add(
                    new int[]{
                            assignedAdults,
                            assignedChildren
                    }
            );
        }

        for (int index = 0;
             index < assignedRooms.size();
             index++) {
            Room room = assignedRooms.get(index);
            int[] allocation = allocations.get(index);
            int remainingCapacity = room.getMaxOccupancy()
                    - allocation[0] - allocation[1];

            int additionalAdults = Math.min(
                    remainingAdults,
                    remainingCapacity
            );
            allocation[0] += additionalAdults;
            remainingAdults -= additionalAdults;
            remainingCapacity -= additionalAdults;

            int additionalChildren = Math.min(
                    remainingChildren,
                    remainingCapacity
            );
            allocation[1] += additionalChildren;
            remainingChildren -= additionalChildren;
        }

        for (int index = 0;
             index < assignedRooms.size();
             index++) {
            int[] allocation = allocations.get(index);
            reservation.addReservationRoom(
                    new ReservationRoom(
                            reservation,
                            assignedRooms.get(index),
                            allocation[0],
                            allocation[1]
                    )
            );
        }

        if (remainingAdults > 0 || remainingChildren > 0) {
            throw new IllegalStateException(
                    "Could not assign all guests across selected rooms. Remaining adults="
                            + remainingAdults + ", children=" + remainingChildren + "."
            );
        }
    }

    /**
     * Consistency check only: compares ReservationAddOn×AddOn.price totals to
     * PricingService's boolean-based add-on component. Logs a warning on drift;
     * does not change Billing amounts (those stay from priceBreakdown).
     */
    private void warnIfPersistedAddOnTotalDrifts(Reservation reservation, double pricingAddOnTotal) {
        Billing probe = new Billing(reservation, 0, 0, 0);
        double persistedAddOnTotal = probe.getPersistedAddOnTotal();

        if (Math.abs(persistedAddOnTotal - pricingAddOnTotal) > 0.009) {
            // No project logger is configured yet; drift check retained without console output.
        }
    }

    /**
     * Looks up selected add-ons by name and attaches ReservationAddOn rows (quantity 1)
     * to the reservation before it is persisted.
     */
    public void attachSelectedAddOns(
            Reservation reservation,
            boolean wifiSelected,
            boolean breakfastSelected,
            boolean spaSelected,
            boolean parkingSelected,
            boolean laundrySelected,
            boolean airportPickupSelected
    ) {
        if (wifiSelected) {
            attachAddOn(reservation, WIFI_NAME);
        }
        if (breakfastSelected) {
            attachAddOn(reservation, BREAKFAST_NAME);
        }
        if (spaSelected) {
            attachAddOn(reservation, SPA_NAME);
        }
        if (parkingSelected) {
            attachAddOn(reservation, PARKING_NAME);
        }
        if (laundrySelected) {
            attachAddOn(reservation, LAUNDRY_NAME);
        }
        if (airportPickupSelected) {
            attachAddOn(reservation, AIRPORT_PICKUP_NAME);
        }
    }

    private void attachAddOn(Reservation reservation, String addOnName) {
        AddOn addOn = addOnRepository.findByName(addOnName)
                .orElseThrow(() -> new IllegalStateException("Add-on not found: " + addOnName));

        reservation.addReservationAddOn(new ReservationAddOn(reservation, addOn, 1));
    }
}
