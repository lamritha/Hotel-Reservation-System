package com.hotelreservation.events;

public interface RoomAvailabilitySubject {

    void subscribe(RoomAvailabilityObserver observer);

    void unsubscribe(RoomAvailabilityObserver observer);

    void publish(RoomAvailabilityEvent event);
}
