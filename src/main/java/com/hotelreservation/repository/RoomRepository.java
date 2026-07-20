package com.hotelreservation.repository;

import com.hotelreservation.entity.Room;
import com.hotelreservation.entity.RoomStatus;
import com.hotelreservation.entity.RoomType;

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

    public List<Room> findAvailableRooms() {
        return executeRead(entityManager ->
                entityManager
                        .createQuery(
                                "SELECT r FROM Room r WHERE r.status = :status",
                                Room.class
                        )
                        .setParameter("status", RoomStatus.AVAILABLE)
                        .getResultList()
        );
    }

    public List<Room> findAvailableRoomsByType(RoomType roomType) {
        return executeRead(entityManager ->
                entityManager
                        .createQuery(
                                "SELECT r FROM Room r WHERE r.status = :status AND r.roomType = :roomType",
                                Room.class
                        )
                        .setParameter("status", RoomStatus.AVAILABLE)
                        .setParameter("roomType", roomType)
                        .getResultList()
        );
    }

    public Room update(Room room) {
        return merge(room);
    }
}
