package com.hotelreservation.service;

import com.hotelreservation.entity.RoomType;
import com.hotelreservation.strategy.PricingStrategy;
import com.hotelreservation.strategy.StandardPricingStrategy;
import com.hotelreservation.strategy.WeekendPricingStrategy;
import com.hotelreservation.util.BookingSession;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Single source of truth for kiosk booking price calculations.
 */
public class PricingService {

    private static final double TAX_RATE = 0.13;

    private static final double WIFI_PRICE = 15;
    private static final double BREAKFAST_PRICE = 20;
    private static final double PARKING_PRICE = 25;
    private static final double SPA_PRICE = 80;
    private static final double LAUNDRY_PRICE = 30;
    private static final double AIRPORT_PICKUP_PRICE = 60;

    public double getTaxRate() {
        return TAX_RATE;
    }

    public long getNumberOfNights() {
        return ChronoUnit.DAYS.between(
                BookingSession.getCheckInDate(),
                BookingSession.getCheckOutDate()
        );
    }

    public long getNumberOfNights(LocalDate checkInDate, LocalDate checkOutDate) {
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    /**
     * Multi-room stay total from BookingSession quantities and date-driven weekend pricing.
     */
    public double calculateRoomTotal() {
        double nightlyRoomCost =
                BookingSession.getSingleRoomQuantity() * RoomType.SINGLE.getBasePrice()
                        + BookingSession.getDoubleRoomQuantity() * RoomType.DOUBLE.getBasePrice()
                        + BookingSession.getDeluxeRoomQuantity() * RoomType.DELUXE.getBasePrice()
                        + BookingSession.getPenthouseRoomQuantity() * RoomType.PENTHOUSE.getBasePrice();

        PricingStrategy pricingStrategy = new WeekendPricingStrategy();
        return pricingStrategy.calculatePrice(
                nightlyRoomCost,
                BookingSession.getCheckInDate(),
                BookingSession.getCheckOutDate()
        );
    }

    /**
     * Strategy pricing for an explicit nightly room cost (Standard vs Weekend).
     */
    public double calculateRoomSubtotal(
            double nightlyRoomCost,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            boolean useWeekendPricing
    ) {
        PricingStrategy pricingStrategy = useWeekendPricing
                ? new WeekendPricingStrategy()
                : new StandardPricingStrategy();

        return pricingStrategy.calculatePrice(nightlyRoomCost, checkInDate, checkOutDate);
    }

    public double calculateAddOnTotal(
            boolean wifi,
            boolean breakfast,
            boolean spa,
            boolean parking,
            boolean laundry,
            boolean airportPickup
    ) {
        long nights = getNumberOfNights();
        double total = 0;

        if (wifi) {
            total += WIFI_PRICE;
        }
        if (breakfast) {
            total += BREAKFAST_PRICE * nights;
        }
        if (parking) {
            total += PARKING_PRICE * nights;
        }
        if (spa) {
            total += SPA_PRICE;
        }
        if (laundry) {
            total += LAUNDRY_PRICE;
        }
        if (airportPickup) {
            total += AIRPORT_PICKUP_PRICE;
        }

        return total;
    }

    public double calculateAddOnTotalFromSession() {
        return calculateAddOnTotal(
                BookingSession.isWifiSelected(),
                BookingSession.isBreakfastSelected(),
                BookingSession.isSpaSelected(),
                BookingSession.isParkingSelected(),
                BookingSession.isLaundrySelected(),
                BookingSession.isAirportPickupSelected()
        );
    }

    public double calculateSubtotal(double roomTotal, double addOnTotal) {
        return roomTotal + addOnTotal;
    }

    public double calculateTax(double subtotal) {
        return subtotal * TAX_RATE;
    }

    public double calculateEstimatedTotal(double subtotal, double taxAmount, double loyaltyDiscount) {
        return subtotal + taxAmount - loyaltyDiscount;
    }

    public double calculateEstimatedTotal(double subtotal, double taxAmount) {
        return calculateEstimatedTotal(subtotal, taxAmount, 0);
    }

    /**
     * Canonical kiosk booking totals from BookingSession.
     * Used by Booking Summary UI and Billing persistence so amounts stay identical.
     */
    public PriceBreakdown calculateSessionPriceBreakdown() {
        return calculateSessionPriceBreakdown(0);
    }

    public PriceBreakdown calculateSessionPriceBreakdown(double loyaltyDiscount) {
        double roomTotal = calculateRoomTotal();
        double addOnTotal = calculateAddOnTotalFromSession();
        double subtotal = calculateSubtotal(roomTotal, addOnTotal);
        double taxAmount = calculateTax(subtotal);
        double estimatedTotal = calculateEstimatedTotal(subtotal, taxAmount, loyaltyDiscount);

        return new PriceBreakdown(roomTotal, addOnTotal, subtotal, taxAmount, loyaltyDiscount, estimatedTotal);
    }

    public String getSelectedAddOnsSummaryFromSession() {
        StringBuilder addOns = new StringBuilder();

        if (BookingSession.isWifiSelected()) {
            addOns.append("Wi-Fi, ");
        }
        if (BookingSession.isBreakfastSelected()) {
            addOns.append("Breakfast, ");
        }
        if (BookingSession.isParkingSelected()) {
            addOns.append("Parking, ");
        }
        if (BookingSession.isSpaSelected()) {
            addOns.append("Spa, ");
        }
        if (BookingSession.isLaundrySelected()) {
            addOns.append("Laundry, ");
        }
        if (BookingSession.isAirportPickupSelected()) {
            addOns.append("Airport Pickup, ");
        }

        if (addOns.length() == 0) {
            return "None";
        }

        return addOns.substring(0, addOns.length() - 2);
    }

    /**
     * Immutable result of session-based booking price calculation.
     */
    public static final class PriceBreakdown {

        private final double roomTotal;
        private final double addOnTotal;
        private final double subtotal;
        private final double taxAmount;
        private final double loyaltyDiscount;
        private final double estimatedTotal;

        public PriceBreakdown(
                double roomTotal,
                double addOnTotal,
                double subtotal,
                double taxAmount,
                double loyaltyDiscount,
                double estimatedTotal
        ) {
            this.roomTotal = roomTotal;
            this.addOnTotal = addOnTotal;
            this.subtotal = subtotal;
            this.taxAmount = taxAmount;
            this.loyaltyDiscount = loyaltyDiscount;
            this.estimatedTotal = estimatedTotal;
        }

        public double getRoomTotal() {
            return roomTotal;
        }

        public double getAddOnTotal() {
            return addOnTotal;
        }

        public double getSubtotal() {
            return subtotal;
        }

        public double getTaxAmount() {
            return taxAmount;
        }

        public double getLoyaltyDiscount() {
            return loyaltyDiscount;
        }

        public double getEstimatedTotal() {
            return estimatedTotal;
        }
    }
}
