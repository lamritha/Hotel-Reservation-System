package com.hotelreservation.service;

import java.time.LocalDate;
import java.util.List;

/**
 * Input used by the administrative create and modify reservation workflows.
 */
public record ReservationRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numAdults,
        int numChildren,
        List<Long> roomIds
) {

    public ReservationRequest {
        roomIds = roomIds == null
                ? List.of()
                : List.copyOf(roomIds);
    }
}
