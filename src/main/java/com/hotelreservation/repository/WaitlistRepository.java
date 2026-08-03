package com.hotelreservation.repository;

import com.hotelreservation.model.RoomType;
import com.hotelreservation.model.WaitlistEntry;
import com.hotelreservation.model.WaitlistStatus;

import java.time.LocalDate;
import java.util.List;

public class WaitlistRepository
        extends AbstractRepository<WaitlistEntry> {

    public WaitlistRepository() {
        super(WaitlistEntry.class);
    }

    public WaitlistEntry save(WaitlistEntry entry) {
        return persist(entry);
    }

    public WaitlistEntry update(WaitlistEntry entry) {
        return merge(entry);
    }

    public WaitlistEntry findById(Long waitlistId) {
        return executeRead(entityManager -> {
            List<WaitlistEntry> results =
                    entityManager.createQuery(
                                    """
                                    SELECT entry
                                    FROM WaitlistEntry entry
                                    JOIN FETCH entry.guest
                                    LEFT JOIN FETCH entry.convertedReservation
                                    WHERE entry.waitlistId = :waitlistId
                                    """,
                                    WaitlistEntry.class
                            )
                            .setParameter(
                                    "waitlistId",
                                    waitlistId
                            )
                            .getResultList();

            return results.isEmpty() ? null : results.getFirst();
        });
    }

    public List<WaitlistEntry> search(
            String keyword,
            WaitlistStatus status
    ) {
        return executeRead(entityManager -> {
            String value = keyword == null
                    ? ""
                    : keyword.trim().toLowerCase();

            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT entry
                    FROM WaitlistEntry entry
                    JOIN FETCH entry.guest guest
                    LEFT JOIN FETCH entry.convertedReservation
                    WHERE 1 = 1
                    """
            );

            if (!value.isBlank()) {
                jpql.append(
                        """
                         AND (
                            LOWER(guest.firstName) LIKE :keyword
                            OR LOWER(guest.lastName) LIKE :keyword
                            OR LOWER(guest.email) LIKE :keyword
                            OR LOWER(guest.phone) LIKE :keyword
                         )
                        """
                );
            }

            if (status != null) {
                jpql.append(" AND entry.status = :status");
            }

            jpql.append(" ORDER BY entry.createdAt DESC");

            var query = entityManager.createQuery(
                    jpql.toString(),
                    WaitlistEntry.class
            );

            if (!value.isBlank()) {
                query.setParameter("keyword", "%" + value + "%");
            }
            if (status != null) {
                query.setParameter("status", status);
            }

            return query.getResultList();
        });
    }

    public List<WaitlistEntry> findWaitingByRoomTypeAndDates(
            RoomType roomType,
            LocalDate availableFrom,
            LocalDate availableTo
    ) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT entry
                                FROM WaitlistEntry entry
                                JOIN FETCH entry.guest
                                WHERE entry.desiredRoomType = :roomType
                                  AND entry.status IN (:waiting, :notified)
                                  AND entry.checkInDate < :availableTo
                                  AND entry.checkOutDate > :availableFrom
                                ORDER BY entry.createdAt
                                """,
                                WaitlistEntry.class
                        )
                        .setParameter("roomType", roomType)
                        .setParameter(
                                "waiting",
                                WaitlistStatus.WAITING
                        )
                        .setParameter(
                                "notified",
                                WaitlistStatus.NOTIFIED
                        )
                        .setParameter(
                                "availableFrom",
                                availableFrom
                        )
                        .setParameter(
                                "availableTo",
                                availableTo
                        )
                        .getResultList()
        );
    }

    public long countActive() {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT COUNT(entry)
                                FROM WaitlistEntry entry
                                WHERE entry.status IN (:waiting, :notified)
                                """,
                                Long.class
                        )
                        .setParameter(
                                "waiting",
                                WaitlistStatus.WAITING
                        )
                        .setParameter(
                                "notified",
                                WaitlistStatus.NOTIFIED
                        )
                        .getSingleResult()
        );
    }
}
