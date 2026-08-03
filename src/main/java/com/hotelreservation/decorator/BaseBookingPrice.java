package com.hotelreservation.decorator;

public final class BaseBookingPrice implements BookingPriceComponent {

    private final double price;
    private final String description;

    public BaseBookingPrice(double price, String description) {
        this.price = price;
        this.description = description;
    }

    @Override
    public double getPrice() {
        return price;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
