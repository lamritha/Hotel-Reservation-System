package com.hotelreservation.decorator;

import com.hotelreservation.config.HotelPolicyConfig;

public final class WifiDecorator extends BookingServiceDecorator {

    public WifiDecorator(BookingPriceComponent wrapped) {
        super(wrapped);
    }

    @Override
    public double getPrice() {
        return wrapped.getPrice() + HotelPolicyConfig.WIFI_PRICE;
    }

    @Override
    public String getDescription() {
        return appendDescription("Wi-Fi");
    }
}
