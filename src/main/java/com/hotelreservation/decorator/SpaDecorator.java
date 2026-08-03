package com.hotelreservation.decorator;

import com.hotelreservation.config.HotelPolicyConfig;

public final class SpaDecorator extends BookingServiceDecorator {

    public SpaDecorator(BookingPriceComponent wrapped) {
        super(wrapped);
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice() + HotelPolicyConfig.SPA_PRICE;
    }

    @Override
    public String getDescription() {
        return appendDescription("Spa");
    }
}
