package com.hotelreservation.repository;

import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomType;

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
     * Rooms of the given type with no CONFIRMED or CHECKED_IN ReservationRoom
     * assignment overlapping [requestedCheckIn, requestedCheckOut).
     */
    public List<Room> findAvailableRoomsByTypeAndDates(
            RoomType roomType,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut
    ) {
        return findAvailableRoomsByTypeAndDates(roomType, requestedCheckIn, requestedCheckOut, null);
    }

    /**
     * Same overlap query as {@link #findAvailableRoomsByTypeAndDates(RoomType, LocalDate, LocalDate)},
     * optionally capped at {@code maxResults} rooms.
     */
    public List<Room> findAvailableRoomsByTypeAndDates(
            RoomType roomType,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut,
            Integer maxResults
    ) {
        return executeRead(entityManager -> {
            var query = entityManager
                    .createQuery(
                            """
                            SELECT r FROM Room r
                            WHERE r.roomType = :roomType
                              AND NOT EXISTS (
                                  SELECT rr FROM ReservationRoom rr
                                  WHERE rr.room = r
                                    AND rr.reservation.status IN (:confirmed, :checkedIn)
                                    AND rr.reservation.checkInDate < :requestedCheckOut
                                    AND rr.reservation.checkOutDate > :requestedCheckIn
                              )
                            """,
                            Room.class
                    )
                    .setParameter("roomType", roomType)
                    .setParameter("confirmed", ReservationStatus.CONFIRMED)
                    .setParameter("checkedIn", ReservationStatus.CHECKED_IN)
                    .setParameter("requestedCheckIn", requestedCheckIn)
                    .setParameter("requestedCheckOut", requestedCheckOut);

            if (maxResults != null && maxResults > 0) {
                query.setMaxResults(maxResults);
            }

            return query.getResultList();
        });
    }

    public Room update(Room room) {
        return merge(room);
    }
}
