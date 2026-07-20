package com.hotelreservation.service;

import com.hotelreservation.entity.Room;

/**
 * Validates guest occupancy against room capacity.
 */
public class OccupancyService {

    public int getTotalGuests(int numAdults, int numChildren) {
        return numAdults + numChildren;
    }

    /**
     * @throws RuntimeException if total guests exceed the room's maximum occupancy
     */
    public void validateOccupancy(Room room, int numAdults, int numChildren) {
        int totalGuests = getTotalGuests(numAdults, numChildren);

        if (totalGuests > room.getMaxOccupancy()) {
            throw new RuntimeException(
                    "Occupancy limit exceeded. Selected room allows maximum "
                            + room.getMaxOccupancy()
                            + " guests."
            );
        }
    }
}
