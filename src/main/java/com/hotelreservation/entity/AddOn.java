package com.hotelreservation.entity;

public enum AddOn {

    WIFI("Wi-Fi", 15.00, false),
    BREAKFAST("Breakfast", 20.00, true),
    PARKING("Parking", 25.00, true),
    SPA("Spa", 80.00, false),
    LAUNDRY("Laundry", 30.00, false),
    AIRPORT_PICKUP("Airport Pickup", 60.00, false);

    private final String displayName;
    private final double price;
    private final boolean perNight;

    AddOn(String displayName, double price, boolean perNight) {
        this.displayName = displayName;
        this.price = price;
        this.perNight = perNight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getPrice() {
        return price;
    }

    public boolean isPerNight() {
        return perNight;
    }
}
