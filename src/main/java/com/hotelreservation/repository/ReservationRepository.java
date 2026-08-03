package com.hotelreservation.repository;

import com.hotelreservation.model.Reservation;
import com.hotelreservation.model.ReservationStatus;
import jakarta.persistence.TypedQuery;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ReservationRepository
        extends AbstractRepository<Reservation> {

    public ReservationRepository() {
        super(Reservation.class);
    }

    public Reservation save(Reservation reservation) {
        return persist(reservation);
    }

    public Reservation findById(Long reservationId) {
        return find(reservationId);
    }

    public Optional<Reservation> findByIdWithDetails(
            Long reservationId
    ) {
        if (reservationId == null) {
            return Optional.empty();
        }

        return executeRead(entityManager -> {
            List<Reservation> results =
                    entityManager.createQuery(
                                    """
                                    SELECT DISTINCT r
                                    FROM Reservation r
                                    JOIN FETCH r.guest g
                                    JOIN FETCH r.room legacyRoom
                                    LEFT JOIN FETCH r.reservationRooms rr
                                    LEFT JOIN FETCH rr.room
                                    WHERE r.reservationId = :reservationId
                                    """,
                                    Reservation.class
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

    public List<Reservation> findAll() {
        return search(null, null, null, null);
    }

    public List<Reservation> findByStatus(
            ReservationStatus status
    ) {
        return search(null, status, null, null);
    }

    public List<Reservation> search(
            String keyword,
            ReservationStatus status,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return executeRead(entityManager -> {
            String normalizedKeyword =
                    keyword == null ? "" : keyword.trim();

            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT DISTINCT r
                    FROM Reservation r
                    JOIN FETCH r.guest g
                    JOIN FETCH r.room legacyRoom
                    LEFT JOIN FETCH r.reservationRooms rr
                    LEFT JOIN FETCH rr.room
                    WHERE 1 = 1
                    """
            );

            Map<String, Object> parameters =
                    new LinkedHashMap<>();

            if (!normalizedKeyword.isBlank()) {
                jpql.append(
                        """
                         AND (
                            LOWER(
                                CONCAT(
                                    CONCAT(g.firstName, ' '),
                                    g.lastName
                                )
                            ) LIKE :keyword
                            OR LOWER(g.email) LIKE :keyword
                            OR LOWER(g.phone) LIKE :keyword
                        """
                );

                Long reservationId =
                        parseReservationId(normalizedKeyword);

                if (reservationId != null) {
                    jpql.append(
                            " OR r.reservationId = :reservationId"
                    );

                    parameters.put(
                            "reservationId",
                            reservationId
                    );
                }

                jpql.append(" )");

                parameters.put(
                        "keyword",
                        "%"
                                + normalizedKeyword.toLowerCase()
                                + "%"
                );
            }

            if (status != null) {
                jpql.append(" AND r.status = :status");
                parameters.put("status", status);
            }

            if (dateFrom != null) {
                jpql.append(
                        " AND r.checkOutDate >= :dateFrom"
                );
                parameters.put("dateFrom", dateFrom);
            }

            if (dateTo != null) {
                jpql.append(
                        " AND r.checkInDate <= :dateTo"
                );
                parameters.put("dateTo", dateTo);
            }

            jpql.append(" ORDER BY r.createdAt DESC");

            TypedQuery<Reservation> query =
                    entityManager.createQuery(
                            jpql.toString(),
                            Reservation.class
                    );

            parameters.forEach(query::setParameter);

            return query.getResultList();
        });
    }

    public Reservation update(Reservation reservation) {
        return merge(reservation);
    }

    public long countAll() {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT COUNT(reservation)
                                FROM Reservation reservation
                                """,
                                Long.class
                        )
                        .getSingleResult()
        );
    }

    public long countByStatusValue(
            ReservationStatus status
    ) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT COUNT(reservation)
                                FROM Reservation reservation
                                WHERE reservation.status = :status
                                """,
                                Long.class
                        )
                        .setParameter("status", status)
                        .getSingleResult()
        );
    }

    public Optional<Reservation> findForFeedbackLookup(
            String lookup
    ) {
        String value = lookup == null ? "" : lookup.trim();
        Long reservationId = parseReservationId(value);

        return executeRead(entityManager -> {
            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT reservation
                    FROM Reservation reservation
                    JOIN FETCH reservation.guest guest
                    WHERE (
                        LOWER(guest.phone) = LOWER(:lookup)
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
                    ORDER BY reservation.checkOutDate DESC,
                             reservation.reservationId DESC
                    """
            );

            var query = entityManager.createQuery(
                            jpql.toString(),
                            Reservation.class
                    )
                    .setParameter("lookup", value)
                    .setMaxResults(1);

            if (reservationId != null) {
                query.setParameter(
                        "reservationId",
                        reservationId
                );
            }

            List<Reservation> results = query.getResultList();
            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public List<Reservation> findOverlappingForReport(
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT DISTINCT reservation
                                FROM Reservation reservation
                                JOIN FETCH reservation.room
                                LEFT JOIN FETCH reservation.reservationRooms assignment
                                LEFT JOIN FETCH assignment.room
                                WHERE reservation.status <> :cancelled
                                  AND reservation.checkInDate < :dateTo
                                  AND reservation.checkOutDate > :dateFrom
                                ORDER BY reservation.checkInDate
                                """,
                                Reservation.class
                        )
                        .setParameter(
                                "cancelled",
                                ReservationStatus.CANCELLED
                        )
                        .setParameter("dateFrom", dateFrom)
                        .setParameter("dateTo", dateTo)
                        .getResultList()
        );
    }

    private Long parseReservationId(String keyword) {
        String value = keyword.trim().toUpperCase();

        if (value.startsWith("RES-")) {
            value = value.substring(4);
        }

        if (!value.matches("\\d+")) {
            return null;
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
