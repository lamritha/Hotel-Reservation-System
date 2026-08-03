package com.hotelreservation.strategy;

import com.hotelreservation.config.HotelPolicyConfig;

import java.time.DayOfWeek;
import java.time.LocalDate;

public class WeekendPricingStrategy implements PricingStrategy {

    @Override
    public double calculatePrice(double nightlyRoomCost, LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null
                || checkOutDate == null
                || !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        double total = 0.0;
        LocalDate currentDate = checkInDate;

        while (currentDate.isBefore(checkOutDate)) {
            DayOfWeek day = currentDate.getDayOfWeek();

            double dayMultiplier =
                    day == DayOfWeek.FRIDAY
                            || day == DayOfWeek.SATURDAY
                            ? HotelPolicyConfig.WEEKEND_MULTIPLIER
                            : HotelPolicyConfig.WEEKDAY_MULTIPLIER;

            total += nightlyRoomCost
                    * dayMultiplier
                    * HotelPolicyConfig
                    .seasonalMultiplier(currentDate);

            currentDate = currentDate.plusDays(1);
        }

        return total;
    }
}
