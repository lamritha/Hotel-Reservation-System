package com.hotelreservation.service;

import com.hotelreservation.entity.*;
import com.hotelreservation.repository.BillingRepository;
import com.hotelreservation.repository.GuestRepository;
import com.hotelreservation.repository.ReservationRepository;
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

    public BookingService() {
        this(
                new RoomAvailabilityService(),
                new OccupancyService(),
                new PricingService(),
                new GuestRepository(),
                new ReservationRepository(),
                new BillingRepository()
        );
    }

    public BookingService(
            RoomAvailabilityService roomAvailabilityService,
            OccupancyService occupancyService,
            PricingService pricingService,
            GuestRepository guestRepository,
            ReservationRepository reservationRepository,
            BillingRepository billingRepository
    ) {
        this.roomAvailabilityService = roomAvailabilityService;
        this.occupancyService = occupancyService;
        this.pricingService = pricingService;
        this.guestRepository = guestRepository;
        this.reservationRepository = reservationRepository;
        this.billingRepository = billingRepository;
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
        Room selectedRoom = roomAvailabilityService.findFirstAvailableRoom(roomType);
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
        reservationRepository.save(reservation);

        Billing billing = new Billing(
                reservation,
                priceBreakdown.getSubtotal(),
                priceBreakdown.getTaxAmount(),
                priceBreakdown.getEstimatedTotal()
        );

        billingRepository.save(billing);

        return reservation;
    }
}
