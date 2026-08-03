package com.hotelreservation.decorator;

public abstract class BookingServiceDecorator
        implements BookingPriceComponent {

    protected final BookingPriceComponent wrapped;

    protected BookingServiceDecorator(
            BookingPriceComponent wrapped
    ) {
        this.wrapped = wrapped;
    }

    protected String appendDescription(String serviceName) {
        String current = wrapped.getDescription();
        if (current == null || current.isBlank()) {
            return serviceName;
        }
        return current + ", " + serviceName;
    }
}
