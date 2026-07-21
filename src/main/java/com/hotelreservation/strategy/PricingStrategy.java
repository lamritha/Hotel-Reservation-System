package com.hotelreservation.strategy;

import java.time.LocalDate;

public interface PricingStrategy {

    double calculatePrice(double nightlyRoomCost, LocalDate checkInDate, LocalDate checkOutDate);
}
