package com.hotelreservation.events;

public interface RoomAvailabilityObserver {

    void onRoomAvailable(RoomAvailabilityEvent event);
}
