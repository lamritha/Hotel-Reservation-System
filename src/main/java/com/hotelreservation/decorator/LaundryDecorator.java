package com.hotelreservation.decorator;

import com.hotelreservation.config.HotelPolicyConfig;

public final class LaundryDecorator extends BookingServiceDecorator {

    public LaundryDecorator(BookingPriceComponent wrapped) {
        super(wrapped);
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice() + HotelPolicyConfig.LAUNDRY_PRICE;
    }

    @Override
    public String getDescription() {
        return appendDescription("Laundry");
    }
}
