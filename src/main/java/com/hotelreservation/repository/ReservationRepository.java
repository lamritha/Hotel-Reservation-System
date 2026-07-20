package com.hotelreservation.repository;

import com.hotelreservation.entity.Reservation;
import com.hotelreservation.entity.ReservationStatus;

import java.util.List;

public class ReservationRepository extends AbstractRepository<Reservation> {

    public ReservationRepository() {
        super(Reservation.class);
    }

    public Reservation save(Reservation reservation) {
        return persist(reservation);
    }

    public Reservation findById(Long reservationId) {
        return find(reservationId);
    }

    public List<Reservation> findAll() {
        return executeRead(entityManager ->
                entityManager
                        .createQuery("SELECT r FROM Reservation r", Reservation.class)
                        .getResultList()
        );
    }

    public List<Reservation> findByStatus(ReservationStatus status) {
        return executeRead(entityManager ->
                entityManager
                        .createQuery(
                                "SELECT r FROM Reservation r WHERE r.status = :status",
                                Reservation.class
                        )
                        .setParameter("status", status)
                        .getResultList()
        );
    }

    public Reservation update(Reservation reservation) {
        return merge(reservation);
    }
}
