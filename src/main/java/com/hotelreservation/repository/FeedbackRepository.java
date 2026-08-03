package com.hotelreservation.repository;

import com.hotelreservation.model.Feedback;
import com.hotelreservation.model.ReservationStatus;
import com.hotelreservation.model.SentimentTag;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FeedbackRepository
        extends AbstractRepository<Feedback> {

    public FeedbackRepository() {
        super(Feedback.class);
    }

    public Feedback save(Feedback feedback) {
        return persist(feedback);
    }

    public Optional<Feedback> findByReservationId(
            Long reservationId
    ) {
        return executeRead(entityManager -> {
            List<Feedback> results =
                    entityManager.createQuery(
                                    """
                                    SELECT feedback
                                    FROM Feedback feedback
                                    JOIN FETCH feedback.guest
                                    JOIN FETCH feedback.reservation
                                    WHERE feedback.reservation.reservationId
                                        = :reservationId
                                    """,
                                    Feedback.class
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

    public List<Feedback> search(
            String guestKeyword,
            Integer rating,
            SentimentTag sentiment,
            LocalDate from,
            LocalDate to
    ) {
        return executeRead(entityManager -> {
            String value = guestKeyword == null
                    ? ""
                    : guestKeyword.trim().toLowerCase();

            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT feedback
                    FROM Feedback feedback
                    JOIN FETCH feedback.guest guest
                    JOIN FETCH feedback.reservation reservation
                    WHERE reservation.status = :checkedOut
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
            if (rating != null) {
                jpql.append(" AND feedback.rating = :rating");
            }
            if (sentiment != null) {
                jpql.append(
                        " AND feedback.sentimentTag = :sentiment"
                );
            }
            if (from != null) {
                jpql.append(
                        " AND feedback.submittedAt >= :from"
                );
            }
            if (to != null) {
                jpql.append(
                        " AND feedback.submittedAt < :toExclusive"
                );
            }

            jpql.append(" ORDER BY feedback.submittedAt DESC");

            var query = entityManager.createQuery(
                    jpql.toString(),
                    Feedback.class
            );
            query.setParameter(
                    "checkedOut",
                    ReservationStatus.CHECKED_OUT
            );

            if (!value.isBlank()) {
                query.setParameter("keyword", "%" + value + "%");
            }
            if (rating != null) {
                query.setParameter("rating", rating);
            }
            if (sentiment != null) {
                query.setParameter("sentiment", sentiment);
            }
            if (from != null) {
                query.setParameter("from", from.atStartOfDay());
            }
            if (to != null) {
                query.setParameter(
                        "toExclusive",
                        to.plusDays(1).atStartOfDay()
                );
            }

            return query.getResultList();
        });
    }
}
