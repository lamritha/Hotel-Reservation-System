package com.hotelreservation.decorator;

import com.hotelreservation.config.HotelPolicyConfig;

public final class BreakfastDecorator extends BookingServiceDecorator {

    private final long nights;

    public BreakfastDecorator(
            BookingPriceComponent wrapped,
            long nights
    ) {
        super(wrapped);
        this.nights = nights;
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice()
                + HotelPolicyConfig.BREAKFAST_PRICE_PER_NIGHT
                * nights;
    }

    @Override
    public String getDescription() {
        return appendDescription("Breakfast");
    }
}
