package com.hotelreservation.strategy;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeekendPricingStrategy implements PricingStrategy {

    private static final double WEEKEND_MULTIPLIER = 1.25;

    @Override
    public double calculatePrice(double nightlyRoomCost, LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        double total = 0.0;
        LocalDate currentDate = checkInDate;

        while (currentDate.isBefore(checkOutDate)) {
            DayOfWeek day = currentDate.getDayOfWeek();

            if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                total += nightlyRoomCost * WEEKEND_MULTIPLIER;
            } else {
                total += nightlyRoomCost;
            }

            currentDate = currentDate.plusDays(1);
        }

        return total;
    }
}
