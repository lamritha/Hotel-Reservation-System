package com.hotelreservation.events;

import com.hotelreservation.model.Room;

import java.time.LocalDate;

public record RoomAvailabilityEvent(
        Room room,
        LocalDate availableFrom
) {
}
