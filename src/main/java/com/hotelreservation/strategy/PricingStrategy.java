package com.hotelreservation.strategy;

import com.hotelreservation.entity.Room;

import java.time.LocalDate;

public interface PricingStrategy {

    double calculatePrice(Room room, LocalDate checkInDate, LocalDate checkOutDate);
}