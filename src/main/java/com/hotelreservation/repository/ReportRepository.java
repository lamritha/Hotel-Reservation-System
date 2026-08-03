package com.hotelreservation.repository;

import com.hotelreservation.model.Billing;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Function;

/**
 * Read-only ORM queries used by the reporting service.
 */
public class ReportRepository {

    public List<Billing> findBillings(
            LocalDate from,
            LocalDate to,
            RoomType roomType
    ) {
        return executeRead(entityManager -> {
            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT DISTINCT billing
                    FROM Billing billing
                    JOIN FETCH billing.reservation reservation
                    JOIN FETCH reservation.guest
                    LEFT JOIN reservation.reservationRooms assignment
                    LEFT JOIN assignment.room assignedRoom
                    WHERE billing.createdAt >= :from
                      AND billing.createdAt < :toExclusive
                    """
            );

            if (roomType != null) {
                jpql.append(
                        """
                         AND (
                            reservation.room.roomType = :roomType
                            OR assignedRoom.roomType = :roomType
                         )
                        """
                );
            }

            jpql.append(" ORDER BY billing.createdAt");

            var query = entityManager.createQuery(
                            jpql.toString(),
                            Billing.class
                    )
                    .setParameter("from", from.atStartOfDay())
                    .setParameter(
                            "toExclusive",
                            to.plusDays(1).atStartOfDay()
                    );

            if (roomType != null) {
                query.setParameter("roomType", roomType);
            }

            return query.getResultList();
        });
    }

    private <T> T executeRead(
            Function<EntityManager, T> work
    ) {
        EntityManager shared =
                JpaUtil.getCurrentEntityManager();
        if (shared != null) {
            return work.apply(shared);
        }

        EntityManager entityManager =
                JpaUtil.getEntityManager();
        try {
            return work.apply(entityManager);
        } finally {
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
