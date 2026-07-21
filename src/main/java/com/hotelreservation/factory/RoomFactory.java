package com.hotelreservation.factory;

import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.model.RoomType;

public class RoomFactory {

    public Room createRoom(RoomType roomType, String roomNumber, int floor) {
        return switch (roomType) {
            case SINGLE -> new Room(
                    roomNumber,
                    floor,
                    RoomType.SINGLE,
                    RoomType.SINGLE.getBasePrice(),
                    RoomType.SINGLE.getMaxOccupancy(),
                    RoomStatus.AVAILABLE
            );

            case DOUBLE -> new Room(
                    roomNumber,
                    floor,
                    RoomType.DOUBLE,
                    180.00,
                    4,
                    RoomStatus.AVAILABLE
            );

            case DELUXE -> new Room(
                    roomNumber,
                    floor,
                    RoomType.DELUXE,
                    250.00,
                    2,
                    RoomStatus.AVAILABLE
            );

            case PENTHOUSE -> new Room(
                    roomNumber,
                    floor,
                    RoomType.PENTHOUSE,
                    500.00,
                    2,
                    RoomStatus.AVAILABLE
            );
        };
    }
}