package com.hotelreservation.model;

public enum RoomType {

    SINGLE(120.00, 2),
    DOUBLE(180.00, 4),
    DELUXE(250.00, 2),
    PENTHOUSE(500.00, 2);

    private final double basePrice;
    private final int maxOccupancy;

    RoomType(double basePrice, int maxOccupancy) {
        this.basePrice = basePrice;
        this.maxOccupancy = maxOccupancy;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }
}