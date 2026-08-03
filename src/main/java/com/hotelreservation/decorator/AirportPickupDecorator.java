package com.hotelreservation.decorator;

import com.hotelreservation.config.HotelPolicyConfig;

public final class AirportPickupDecorator
        extends BookingServiceDecorator {

    public AirportPickupDecorator(
            BookingPriceComponent wrapped
    ) {
        super(wrapped);
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice()
                + HotelPolicyConfig.AIRPORT_PICKUP_PRICE;
    }

    @Override
    public String getDescription() {
        return appendDescription("Airport Pickup");
    }
}
