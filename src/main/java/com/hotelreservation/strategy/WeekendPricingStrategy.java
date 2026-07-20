package com.hotelreservation.strategy;

import com.hotelreservation.entity.Room;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeekendPricingStrategy implements PricingStrategy {

    private static final double WEEKEND_MULTIPLIER = 1.25;

    @Override
    public double calculatePrice(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        if (!checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        double total = 0.0;
        LocalDate currentDate = checkInDate;

        while (currentDate.isBefore(checkOutDate)) {
            DayOfWeek day = currentDate.getDayOfWeek();

            if (day == DayOfWeek.FRIDAY || day == DayOfWeek.SATURDAY) {
                total += room.getBasePrice() * WEEKEND_MULTIPLIER;
            } else {
                total += room.getBasePrice();
            }

            currentDate = currentDate.plusDays(1);
        }

        return total;
    }
}