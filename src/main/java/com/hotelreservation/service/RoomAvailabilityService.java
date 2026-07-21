package com.hotelreservation.service;

import com.hotelreservation.entity.Room;
import com.hotelreservation.entity.RoomType;
import com.hotelreservation.repository.RoomRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Selects available rooms for booking based on reservation date-range overlap.
 * Does not change room status.
 */
public class RoomAvailabilityService {

    private final RoomRepository roomRepository;

    public RoomAvailabilityService() {
        this(new RoomRepository());
    }

    public RoomAvailabilityService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    /**
     * Returns the first room of the given type with no overlapping CONFIRMED reservation
     * for [checkInDate, checkOutDate).
     *
     * @throws RuntimeException if no available room exists for the type and dates
     */
    public Room findFirstAvailableRoom(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate) {
        List<Room> availableRooms = roomRepository.findAvailableRoomsByTypeAndDates(
                roomType,
                checkInDate,
                checkOutDate
        );

        if (availableRooms.isEmpty()) {
            throw new RuntimeException("No available room found for room type: " + roomType
                    + " between " + checkInDate + " and " + checkOutDate);
        }

        return availableRooms.get(0);
    }

    /**
     * Counts rooms of the given type with no overlapping CONFIRMED reservation
     * for [checkInDate, checkOutDate).
     */
    public int countAvailableRooms(RoomType roomType, LocalDate checkInDate, LocalDate checkOutDate) {
        return roomRepository.findAvailableRoomsByTypeAndDates(roomType, checkInDate, checkOutDate).size();
    }

    /**
     * Checks whether requested quantities can be fulfilled for the stay dates.
     *
     * @return null if enough rooms are available; otherwise a user-friendly error message
     */
    public String validateRequestedQuantities(
            int singleQuantity,
            int doubleQuantity,
            int deluxeQuantity,
            int penthouseQuantity,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        StringBuilder errors = new StringBuilder();

        appendInsufficientAvailability(errors, RoomType.SINGLE, singleQuantity, "Single", checkInDate, checkOutDate);
        appendInsufficientAvailability(errors, RoomType.DOUBLE, doubleQuantity, "Double", checkInDate, checkOutDate);
        appendInsufficientAvailability(errors, RoomType.DELUXE, deluxeQuantity, "Deluxe", checkInDate, checkOutDate);
        appendInsufficientAvailability(errors, RoomType.PENTHOUSE, penthouseQuantity, "Penthouse", checkInDate, checkOutDate);

        if (errors.isEmpty()) {
            return null;
        }

        return errors.toString().trim();
    }

    private void appendInsufficientAvailability(
            StringBuilder errors,
            RoomType roomType,
            int requestedQuantity,
            String displayName,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        if (requestedQuantity <= 0) {
            return;
        }

        int availableCount = countAvailableRooms(roomType, checkInDate, checkOutDate);

        if (requestedQuantity > availableCount) {
            if (!errors.isEmpty()) {
                errors.append('\n');
            }

            errors.append("Only ")
                    .append(availableCount)
                    .append(" ")
                    .append(displayName)
                    .append(" room(s) available, but you requested ")
                    .append(requestedQuantity)
                    .append(".");
        }
    }
}
