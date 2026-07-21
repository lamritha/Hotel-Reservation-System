package com.hotelreservation.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reservation_addons")
public class ReservationAddOn {

    @EmbeddedId
    private ReservationAddOnId id = new ReservationAddOnId();

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("reservationID")
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @MapsId("addonID")
    @JoinColumn(name = "addon_id", nullable = false)
    private AddOn addOn;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public ReservationAddOn() {
    }

    public ReservationAddOn(Reservation reservation, AddOn addOn, int quantity) {
        this.reservation = reservation;
        this.addOn = addOn;
        this.quantity = quantity;
        this.id = new ReservationAddOnId(
                reservation != null ? reservation.getReservationId() : null,
                addOn != null ? addOn.getAddonID() : null
        );
    }

    public ReservationAddOnId getId() {
        return id;
    }

    public void setId(ReservationAddOnId id) {
        this.id = id;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    public AddOn getAddOn() {
        return addOn;
    }

    public void setAddOn(AddOn addOn) {
        this.addOn = addOn;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
