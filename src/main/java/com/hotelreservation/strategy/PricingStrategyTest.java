package com.hotelreservation.strategy;

import com.hotelreservation.entity.Room;
import com.hotelreservation.entity.RoomStatus;
import com.hotelreservation.entity.RoomType;

import java.time.LocalDate;

public class PricingStrategyTest {

    public static void main(String[] args) {
        Room deluxeRoom = new Room(
                "301",
                3,
                RoomType.DELUXE,
                250.00,
                2,
                RoomStatus.AVAILABLE
        );

        LocalDate checkInDate = LocalDate.of(2026, 7, 17);  // Friday
        LocalDate checkOutDate = LocalDate.of(2026, 7, 20); // Monday

        PricingStrategy standardPricing = new StandardPricingStrategy();
        PricingStrategy weekendPricing = new WeekendPricingStrategy();

        double standardTotal = standardPricing.calculatePrice(deluxeRoom, checkInDate, checkOutDate);
        double weekendTotal = weekendPricing.calculatePrice(deluxeRoom, checkInDate, checkOutDate);

        System.out.println("Room Type: " + deluxeRoom.getRoomType());
        System.out.println("Base Price: CAD " + deluxeRoom.getBasePrice());
        System.out.println("Check-in Date: " + checkInDate);
        System.out.println("Check-out Date: " + checkOutDate);
        System.out.println("Standard Pricing Total: CAD " + standardTotal);
        System.out.println("Weekend Pricing Total: CAD " + weekendTotal);
    }
}