package com.hotelreservation.service;

import com.hotelreservation.entity.*;
import com.hotelreservation.repository.AddOnRepository;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.JpaUtil;

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
                new AddOnRepository()
        );
    }

    public BookingService(
            RoomAvailabilityService roomAvailabilityService,
            OccupancyService occupancyService,
            PricingService pricingService,
            GuestRepository guestRepository,
            ReservationRepository reservationRepository,
            BillingRepository billingRepository,
            AddOnRepository addOnRepository
    ) {
        this.roomAvailabilityService = roomAvailabilityService;
        this.occupancyService = occupancyService;
        this.pricingService = pricingService;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.billingRepository = billingRepository;
        this.addOnRepository = addOnRepository;
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
                groupBooking
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

        Billing billing = new Billing(
                reservation,
                priceBreakdown.getSubtotal(),
                priceBreakdown.getTaxAmount(),
                priceBreakdown.getEstimatedTotal()
        );

        billingRepository.save(billing);

        System.out.println(String.format(
                "Billing saved: subtotal=%.2f tax=%.2f total=%.2f | persistedAddOnTotal=%.2f | pricingAddOnTotal=%.2f | rooms=%d",
                billing.getSubtotal(),
                billing.getTaxAmount(),
                billing.getTotalAmount(),
                billing.getPersistedAddOnTotal(),
                priceBreakdown.getAddOnTotal(),
                assignedRooms.size()
        ));

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

        for (Room room : assignedRooms) {
            int capacity = room.getMaxOccupancy();

            int assignedAdults = Math.min(remainingAdults, capacity);
            remainingAdults -= assignedAdults;

            int remainingCapacity = capacity - assignedAdults;
            int assignedChildren = Math.min(remainingChildren, remainingCapacity);
            remainingChildren -= assignedChildren;

            reservation.addReservationRoom(
                    new ReservationRoom(reservation, room, assignedAdults, assignedChildren)
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
            System.out.println(String.format(
                    "WARNING: Persisted ReservationAddOn total (%.2f) does not match "
                            + "PricingService add-on total (%.2f). Seeded AddOn.price values "
                            + "may have drifted from PricingService hardcoded prices.",
                    persistedAddOnTotal,
                    pricingAddOnTotal
            ));
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
