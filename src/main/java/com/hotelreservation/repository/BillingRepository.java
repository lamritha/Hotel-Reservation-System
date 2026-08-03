package com.hotelreservation.repository;

import com.hotelreservation.model.Billing;

import java.util.List;
import java.util.Optional;

public class BillingRepository extends AbstractRepository<Billing> {

    public BillingRepository() {
        super(Billing.class);
    }

    public Billing save(Billing billing) {
        return persist(billing);
    }

    public Billing update(Billing billing) {
        return merge(billing);
    }

    public Billing findById(Long billingId) {
        return find(billingId);
    }

    public Optional<Billing> findByReservationId(
            Long reservationId
    ) {
        if (reservationId == null) {
            return Optional.empty();
        }

        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            """
                            SELECT billing
                            FROM Billing billing
                            WHERE billing.reservation.reservationId
                                = :reservationId
                            """,
                            Billing.class
                    )
                    .setParameter(
                            "reservationId",
                            reservationId
                    )
                    .getResultList();

            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public Optional<Billing> findByReservationIdWithGuest(
            Long reservationId
    ) {
        if (reservationId == null) {
            return Optional.empty();
        }

        return executeRead(entityManager -> {
            List<Billing> results =
                    entityManager.createQuery(
                                    """
                                    SELECT billing
                                    FROM Billing billing
                                    JOIN FETCH billing.reservation reservation
                                    JOIN FETCH reservation.guest
                                    WHERE reservation.reservationId
                                        = :reservationId
                                    """,
                                    Billing.class
                            )
                            .setParameter(
                                    "reservationId",
                                    reservationId
                            )
                            .getResultList();

            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public List<Billing> search(String keyword) {
        return executeRead(entityManager -> {
            String value = keyword == null
                    ? ""
                    : keyword.trim().toLowerCase();

            Long reservationId = parseReservationId(value);

            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT billing
                    FROM Billing billing
                    JOIN FETCH billing.reservation reservation
                    JOIN FETCH reservation.guest guest
                    WHERE (
                        :blank = true
                        OR LOWER(guest.firstName) LIKE :pattern
                        OR LOWER(guest.lastName) LIKE :pattern
                        OR LOWER(guest.email) LIKE :pattern
                        OR LOWER(guest.phone) LIKE :pattern
                    """
            );

            if (reservationId != null) {
                jpql.append(
                        " OR reservation.reservationId = :reservationId"
                );
            }

            jpql.append(
                    """
                     )
                    ORDER BY billing.createdAt DESC
                    """
            );

            var query = entityManager.createQuery(
                            jpql.toString(),
                            Billing.class
                    )
                    .setParameter("blank", value.isBlank())
                    .setParameter("pattern", "%" + value + "%");

            if (reservationId != null) {
                query.setParameter(
                        "reservationId",
                        reservationId
                );
            }

            return query.getResultList();
        });
    }

    public long countOutstanding() {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                "SELECT COUNT(billing) FROM Billing billing",
                                Long.class
                        )
                        .getSingleResult()
        );
    }

    private Long parseReservationId(String keyword) {
        String value = keyword == null
                ? ""
                : keyword.trim().toUpperCase();
        if (value.startsWith("RES-")) {
            value = value.substring(4);
        }
        try {
            return value.matches("\\d+")
                    ? Long.valueOf(value)
                    : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
