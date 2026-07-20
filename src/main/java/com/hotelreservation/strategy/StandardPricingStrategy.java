package com.hotelreservation.strategy;

import com.hotelreservation.entity.Room;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class StandardPricingStrategy implements PricingStrategy {

    @Override
    public double calculatePrice(Room room, LocalDate checkInDate, LocalDate checkOutDate) {
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);

        if (nights <= 0) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        return room.getBasePrice() * nights;
    }
}