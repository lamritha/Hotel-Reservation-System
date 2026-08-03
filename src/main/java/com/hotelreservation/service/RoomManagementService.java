package com.hotelreservation.service;

import com.hotelreservation.events.AvailabilityEventPublisher;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.repository.RoomRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.util.AppLogger;

import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoomManagementService {

    private static final Logger LOGGER =
            AppLogger.getLogger(
                    RoomManagementService.class
            );

    private final RoomRepository roomRepository;
    private final AvailabilityEventPublisher publisher;
    private final AdminSession adminSession;

    public RoomManagementService(
            RoomRepository roomRepository,
            AvailabilityEventPublisher publisher,
            AdminSession adminSession
    ) {
        this.roomRepository = roomRepository;
        this.publisher = publisher;
        this.adminSession = adminSession;
    }

    public List<Room> search(
            String keyword,
            RoomType type,
            RoomStatus status
    ) {
        adminSession.requireCurrentUser();
        return roomRepository.search(keyword, type, status);
    }

    public Room changeStatus(
            Room room,
            RoomStatus newStatus
    ) {
        adminSession.requireCurrentUser();
        if (room == null) {
            throw new IllegalArgumentException(
                    "Select a room first."
            );
        }
        if (newStatus == null) {
            throw new IllegalArgumentException(
                    "Select a room status."
            );
        }

        RoomStatus oldStatus = room.getStatus();
        room.setStatus(newStatus);
        Room updated = roomRepository.update(room);

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                "ROOM_STATUS_CHANGED",
                "Room",
                String.valueOf(room.getRoomId()),
                "Room " + room.getRoomNumber()
                        + " changed from " + oldStatus
                        + " to " + newStatus + "."
        );

        if (newStatus == RoomStatus.AVAILABLE
                && oldStatus != RoomStatus.AVAILABLE) {
            publisher.roomBecameAvailable(
                    updated,
                    LocalDate.now()
            );
        }

        return updated;
    }
}
