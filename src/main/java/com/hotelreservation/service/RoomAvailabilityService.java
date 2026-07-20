package com.hotelreservation.service;

import com.hotelreservation.entity.Room;
import com.hotelreservation.entity.RoomType;
import com.hotelreservation.repository.RoomRepository;

import java.util.List;

/**
 * Selects available rooms for booking. Does not change room status.
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
     * Returns the first available room of the given type.
     *
     * @throws RuntimeException if no available room exists for the type
     */
    public Room findFirstAvailableRoom(RoomType roomType) {
        List<Room> availableRooms = roomRepository.findAvailableRoomsByType(roomType);

        if (availableRooms.isEmpty()) {
            throw new RuntimeException("No available room found for room type: " + roomType);
        }

        return availableRooms.get(0);
    }

    /**
     * Counts rooms currently marked AVAILABLE for the given type.
     */
    public int countAvailableRooms(RoomType roomType) {
        return roomRepository.findAvailableRoomsByType(roomType).size();
    }

    /**
     * Checks whether requested quantities can be fulfilled with currently available rooms.
     *
     * @return null if enough rooms are available; otherwise a user-friendly error message
     */
    public String validateRequestedQuantities(
            int singleQuantity,
            int doubleQuantity,
            int deluxeQuantity,
            int penthouseQuantity
    ) {
        StringBuilder errors = new StringBuilder();

        appendInsufficientAvailability(errors, RoomType.SINGLE, singleQuantity, "Single");
        appendInsufficientAvailability(errors, RoomType.DOUBLE, doubleQuantity, "Double");
        appendInsufficientAvailability(errors, RoomType.DELUXE, deluxeQuantity, "Deluxe");
        appendInsufficientAvailability(errors, RoomType.PENTHOUSE, penthouseQuantity, "Penthouse");

        if (errors.isEmpty()) {
            return null;
        }

        return errors.toString().trim();
    }

    private void appendInsufficientAvailability(
            StringBuilder errors,
            RoomType roomType,
            int requestedQuantity,
            String displayName
    ) {
        if (requestedQuantity <= 0) {
            return;
        }

        int availableCount = countAvailableRooms(roomType);

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
