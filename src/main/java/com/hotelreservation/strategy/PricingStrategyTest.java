package com.hotelreservation.strategy;

import java.time.LocalDate;

public class PricingStrategyTest {

    public static void main(String[] args) {
        double nightlyRoomCost = 250.00; // Deluxe base price

        LocalDate checkInDate = LocalDate.of(2026, 7, 17);  // Friday
        LocalDate checkOutDate = LocalDate.of(2026, 7, 20); // Monday

        PricingStrategy standardPricing = new StandardPricingStrategy();
        PricingStrategy weekendPricing = new WeekendPricingStrategy();

        double standardTotal = standardPricing.calculatePrice(nightlyRoomCost, checkInDate, checkOutDate);
        double weekendTotal = weekendPricing.calculatePrice(nightlyRoomCost, checkInDate, checkOutDate);

        System.out.println("Nightly Room Cost: CAD " + nightlyRoomCost);
        System.out.println("Check-in Date: " + checkInDate);
        System.out.println("Check-out Date: " + checkOutDate);
        System.out.println("Standard Pricing Total: CAD " + standardTotal);
        System.out.println("Weekend Pricing Total: CAD " + weekendTotal);
    }
}
