package com.hotelreservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ReservationAddOnId implements Serializable {

    @Column(name = "reservation_id")
    private Long reservationID;

    @Column(name = "addon_id")
    private Long addonID;

    public ReservationAddOnId() {
    }

    public ReservationAddOnId(Long reservationID, Long addonID) {
        this.reservationID = reservationID;
        this.addonID = addonID;
    }

    public Long getReservationID() {
        return reservationID;
    }

    public void setReservationID(Long reservationID) {
        this.reservationID = reservationID;
    }

    public Long getAddonID() {
        return addonID;
    }

    public void setAddonID(Long addonID) {
        this.addonID = addonID;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ReservationAddOnId that)) {
            return false;
        }
        return Objects.equals(reservationID, that.reservationID)
                && Objects.equals(addonID, that.addonID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reservationID, addonID);
    }
}
