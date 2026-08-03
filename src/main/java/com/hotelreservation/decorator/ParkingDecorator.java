package com.hotelreservation.decorator;

import com.hotelreservation.config.HotelPolicyConfig;

public final class ParkingDecorator extends BookingServiceDecorator {

    private final long nights;

    public ParkingDecorator(
            BookingPriceComponent wrapped,
            long nights
    ) {
        super(wrapped);
        this.nights = nights;
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice()
                + HotelPolicyConfig.PARKING_PRICE_PER_NIGHT
                * nights;
    }

    @Override
    public String getDescription() {
        return appendDescription("Parking");
    }
}
