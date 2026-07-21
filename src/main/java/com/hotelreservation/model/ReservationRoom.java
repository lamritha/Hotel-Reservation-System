package com.hotelreservation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reservation_rooms")
public class ReservationRoom {

    @EmbeddedId
    private ReservationRoomId id = new ReservationRoomId();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("reservationID")
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("roomID")
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "assigned_adults", nullable = false)
    private int assignedAdults;

    @Column(name = "assigned_children", nullable = false)
    private int assignedChildren;

    public ReservationRoom() {
    }

    public ReservationRoom(Reservation reservation, Room room, int assignedAdults, int assignedChildren) {
        this.reservation = reservation;
        this.room = room;
        this.assignedAdults = assignedAdults;
        this.assignedChildren = assignedChildren;
        this.id = new ReservationRoomId(
                reservation != null ? reservation.getReservationId() : null,
                room != null ? room.getRoomId() : null
        );
    }

    public ReservationRoomId getId() {
        return id;
    }

    public void setId(ReservationRoomId id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public int getAssignedAdults() {
        return assignedAdults;
    }

    public void setAssignedAdults(int assignedAdults) {
        this.assignedAdults = assignedAdults;
    }

    public int getAssignedChildren() {
        return assignedChildren;
    }

    public void setAssignedChildren(int assignedChildren) {
        this.assignedChildren = assignedChildren;
    }
}
