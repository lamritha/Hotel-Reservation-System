package com.hotelreservation.service;

import com.hotelreservation.entity.*;
import com.hotelreservation.repository.AddOnRepository;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.ReservationRepository;
import com.hotelreservation.util.BookingSession;
import com.hotelreservation.util.JpaUtil;

import java.time.LocalDate;

/**
 * Orchestrates kiosk booking completion: room selection, occupancy checks,
 * pricing via PricingService, and persistence of Guest, Reservation, and Billing.
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
            RoomType roomType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numAdults,
            int numChildren,
            boolean groupBooking,
            PaymentMethod paymentMethod,
            boolean useWeekendPricing
    ) {
        try {
            return JpaUtil.executeInTransaction(() ->
                    completeBookingInTransaction(
                            guest,
                            roomType,
                            checkInDate,
                            checkOutDate,
                            numAdults,
                            numChildren,
                            groupBooking,
                            paymentMethod,
                            useWeekendPricing
                    )
            );
        } catch (Exception e) {
            throw new RuntimeException("Booking failed", e);
        }
    }

    private Reservation completeBookingInTransaction(
            Guest guest,
            RoomType roomType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numAdults,
            int numChildren,
            boolean groupBooking,
            PaymentMethod paymentMethod,
            boolean useWeekendPricing
    ) {
        Room selectedRoom = roomAvailabilityService.findFirstAvailableRoom(
                roomType,
                checkInDate,
                checkOutDate
        );
        occupancyService.validateOccupancy(selectedRoom, numAdults, numChildren);

        PricingService.PriceBreakdown priceBreakdown = pricingService.calculateSessionPriceBreakdown();

        guestRepository.save(guest);

        Reservation reservation = new Reservation(
                guest,
                selectedRoom,
                checkInDate,
                checkOutDate,
                numAdults,
                numChildren,
                groupBooking
        );

        reservation.setStatus(ReservationStatus.CONFIRMED);

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
                "Billing saved: subtotal=%.2f tax=%.2f total=%.2f | persistedAddOnTotal=%.2f | pricingAddOnTotal=%.2f",
                billing.getSubtotal(),
                billing.getTaxAmount(),
                billing.getTotalAmount(),
                billing.getPersistedAddOnTotal(),
                priceBreakdown.getAddOnTotal()
        ));

        return reservation;
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
