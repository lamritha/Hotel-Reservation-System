package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    private Long waitlistId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Enumerated(EnumType.STRING)
    @Column(name = "desired_room_type", nullable = false, length = 20)
    private RoomType desiredRoomType;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "num_adults", nullable = false)
    private int numAdults;

    @Column(name = "num_children", nullable = false)
    private int numChildren;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WaitlistStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "converted_reservation_id")
    private Reservation convertedReservation;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public WaitlistEntry() {
        status = WaitlistStatus.WAITING;
    }

    public WaitlistEntry(
            Guest guest,
            RoomType desiredRoomType,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            int numAdults,
            int numChildren
    ) {
        this.guest = guest;
        this.desiredRoomType = desiredRoomType;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numAdults = numAdults;
        this.numChildren = numChildren;
        this.status = WaitlistStatus.WAITING;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
        if (status == null) {
            status = WaitlistStatus.WAITING;
        }
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getWaitlistId() {
        return waitlistId;
    }

    public Guest getGuest() {
        return guest;
    }

    public RoomType getDesiredRoomType() {
        return desiredRoomType;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public int getNumAdults() {
        return numAdults;
    }

    public int getNumChildren() {
        return numChildren;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public Reservation getConvertedReservation() {
        return convertedReservation;
    }

    public void setConvertedReservation(Reservation convertedReservation) {
        this.convertedReservation = convertedReservation;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
