package com.hotelreservation.repository;

import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
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

    public List<Room> search(
            String keyword,
            RoomType roomType,
            RoomStatus roomStatus
    ) {
        return executeRead(entityManager -> {
            String value = keyword == null
                    ? ""
                    : keyword.trim().toLowerCase();

            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT room
                    FROM Room room
                    WHERE 1 = 1
                    """
            );

            if (!value.isBlank()) {
                jpql.append(
                        """
                         AND (
                            LOWER(room.roomNumber) LIKE :keyword
                            OR CAST(room.floor AS String) LIKE :keyword
                         )
                        """
                );
            }
            if (roomType != null) {
                jpql.append(" AND room.roomType = :roomType");
            }
            if (roomStatus != null) {
                jpql.append(" AND room.status = :roomStatus");
            }

            jpql.append(" ORDER BY room.roomNumber");

            var query = entityManager.createQuery(
                    jpql.toString(),
                    Room.class
            );
            if (!value.isBlank()) {
                query.setParameter("keyword", "%" + value + "%");
            }
            if (roomType != null) {
                query.setParameter("roomType", roomType);
            }
            if (roomStatus != null) {
                query.setParameter("roomStatus", roomStatus);
            }
            return query.getResultList();
        });
    }

    public List<Room> findAll() {
        return search(null, null, null);
    }

    public long countByStatus(RoomStatus status) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT COUNT(room)
                                FROM Room room
                                WHERE room.status = :status
                                """,
                                Long.class
                        )
                        .setParameter("status", status)
                        .getSingleResult()
        );
    }

    public List<Room> findAvailableRoomsByTypeAndDates(
            RoomType roomType,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut
    ) {
        return findAvailableRooms(
                roomType,
                requestedCheckIn,
                requestedCheckOut,
                null,
                null
        );
    }

    public List<Room> findAvailableRoomsByTypeAndDates(
            RoomType roomType,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut,
            Integer maxResults
    ) {
        return findAvailableRooms(
                roomType,
                requestedCheckIn,
                requestedCheckOut,
                null,
                maxResults
        );
    }

    /**
     * Returns every non-maintenance room that has no overlapping confirmed
     * or checked-in reservation. The reservation being edited can be excluded
     * so its currently assigned rooms remain selectable.
     */
    public List<Room> findAvailableRoomsForDates(
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut,
            Long excludedReservationId
    ) {
        return findAvailableRooms(
                null,
                requestedCheckIn,
                requestedCheckOut,
                excludedReservationId,
                null
        );
    }

    private List<Room> findAvailableRooms(
            RoomType roomType,
            LocalDate requestedCheckIn,
            LocalDate requestedCheckOut,
            Long excludedReservationId,
            Integer maxResults
    ) {
        return executeRead(entityManager -> {
            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT room
                    FROM Room room
                    WHERE room.status <> :maintenance
                    """
            );

            if (roomType != null) {
                jpql.append(
                        " AND room.roomType = :roomType"
                );
            }

            jpql.append(
                    """
                     AND NOT EXISTS (
                        SELECT reservation
                        FROM Reservation reservation
                        WHERE reservation.room = room
                          AND reservation.status
                              IN (:confirmed, :checkedIn)
                    """
            );

            if (excludedReservationId != null) {
                jpql.append(
                        """
                         AND reservation.reservationId
                             <> :excludedReservationId
                        """
                );
            }

            jpql.append(
                    """
                       AND reservation.checkInDate
                           < :requestedCheckOut
                       AND reservation.checkOutDate
                           > :requestedCheckIn
                     )
                     AND NOT EXISTS (
                        SELECT reservationRoom
                        FROM ReservationRoom reservationRoom
                        WHERE reservationRoom.room = room
                          AND reservationRoom.reservation.status
                              IN (:confirmed, :checkedIn)
                    """
            );

            if (excludedReservationId != null) {
                jpql.append(
                        """
                         AND reservationRoom.reservation.reservationId
                             <> :excludedReservationId
                        """
                );
            }

            jpql.append(
                    """
                       AND reservationRoom.reservation.checkInDate
                           < :requestedCheckOut
                       AND reservationRoom.reservation.checkOutDate
                           > :requestedCheckIn
                     )
                    ORDER BY room.roomNumber
                    """
            );

            var query = entityManager
                    .createQuery(
                            jpql.toString(),
                            Room.class
                    )
                    .setParameter(
                            "maintenance",
                            RoomStatus.MAINTENANCE
                    )
                    .setParameter(
                            "confirmed",
                            ReservationStatus.CONFIRMED
                    )
                    .setParameter(
                            "checkedIn",
                            ReservationStatus.CHECKED_IN
                    )
                    .setParameter(
                            "requestedCheckIn",
                            requestedCheckIn
                    )
                    .setParameter(
                            "requestedCheckOut",
                            requestedCheckOut
                    );

            if (roomType != null) {
                query.setParameter("roomType", roomType);
            }

            if (excludedReservationId != null) {
                query.setParameter(
                        "excludedReservationId",
                        excludedReservationId
                );
            }

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
