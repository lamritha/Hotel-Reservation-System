package com.hotelreservation.repository;

import com.hotelreservation.entity.ReservationStatus;
import com.hotelreservation.entity.Room;
import com.hotelreservation.entity.RoomType;

import java.time.LocalDate;
import java.util.List;

public class RoomRepository extends AbstractRepository<Room> {

    public RoomRepository() {
        super(Room.class);
    }

    public Room save(Room room) {
        return persist(room);
    }

    public Room findById(Long roomId) {
        return find(roomId);
    }

    /**
     * Rooms of the given type with no CONFIRMED or CHECKED_IN reservation overlapping
     * [requestedCheckIn, requestedCheckOut).
     */
    public List<Room> findAvailableRoomsByTypeAndDates(
            RoomType roomType,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut
    ) {
        return executeRead(entityManager ->
                entityManager
                        .createQuery(
                                """
                                SELECT r FROM Room r
                                WHERE r.roomType = :roomType
                                  AND NOT EXISTS (
                                      SELECT res FROM Reservation res
                                      WHERE res.room = r
                                        AND res.status IN (:confirmed, :checkedIn)
                                        AND res.checkInDate < :requestedCheckOut
                                        AND res.checkOutDate > :requestedCheckIn
                                  )
                                """,
                                Room.class
                        )
                        .setParameter("roomType", roomType)
                        .setParameter("confirmed", ReservationStatus.CONFIRMED)
                        .setParameter("checkedIn", ReservationStatus.CHECKED_IN)
                        .setParameter("requestedCheckIn", requestedCheckIn)
                        .setParameter("requestedCheckOut", requestedCheckOut)
                        .getResultList()
        );
    }

    public Room update(Room room) {
        return merge(room);
    }
}
