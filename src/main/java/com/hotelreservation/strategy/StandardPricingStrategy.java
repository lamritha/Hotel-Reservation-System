package com.hotelreservation.strategy;

import com.hotelreservation.config.HotelPolicyConfig;

import java.time.LocalDate;

public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public double calculatePrice(double nightlyRoomCost, LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null
                || checkOutDate == null
                || !checkOutDate.isAfter(checkInDate)) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        double total = 0;
        LocalDate date = checkInDate;

        while (date.isBefore(checkOutDate)) {
            total += nightlyRoomCost
                    * HotelPolicyConfig.WEEKDAY_MULTIPLIER
                    * HotelPolicyConfig.seasonalMultiplier(date);
            date = date.plusDays(1);
        }

        return total;
    }
}
