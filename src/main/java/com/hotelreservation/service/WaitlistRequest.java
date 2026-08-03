package com.hotelreservation.service;

import com.hotelreservation.model.RoomType;

import java.time.LocalDate;

public record WaitlistRequest(
        String firstName,
        String lastName,
        String email,
        String phone,
        String address,
        RoomType roomType,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        int numAdults,
        int numChildren
) {
}
