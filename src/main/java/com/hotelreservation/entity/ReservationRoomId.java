package com.hotelreservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ReservationRoomId implements Serializable {

    @Column(name = "reservation_id")
    private Long reservationID;

    @Column(name = "room_id")
    private Long roomID;

    public ReservationRoomId() {
    }

    public ReservationRoomId(Long reservationID, Long roomID) {
        this.reservationID = reservationID;
        this.roomID = roomID;
    }

    public Long getReservationID() {
        return reservationID;
    }

    public void setReservationID(Long reservationID) {
        this.reservationID = reservationID;
    }

    public Long getRoomID() {
        return roomID;
    }

    public void setRoomID(Long roomID) {
        this.roomID = roomID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationRoomId that)) {
            return false;
        }
        return Objects.equals(reservationID, that.reservationID)
                && Objects.equals(roomID, that.roomID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationID, roomID);
    }
}
