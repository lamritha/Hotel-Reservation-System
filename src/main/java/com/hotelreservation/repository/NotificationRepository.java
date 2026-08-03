package com.hotelreservation.repository;

import com.hotelreservation.model.AdminNotification;
import com.hotelreservation.model.NotificationType;

import java.util.List;

public class NotificationRepository
        extends AbstractRepository<AdminNotification> {

    public NotificationRepository() {
        super(AdminNotification.class);
    }

    public AdminNotification save(
            AdminNotification notification
    ) {
        return persist(notification);
    }

    public AdminNotification update(
            AdminNotification notification
    ) {
        return merge(notification);
    }

    public List<AdminNotification> search(
            String recipient,
            String keyword,
            NotificationType type,
            Boolean read
    ) {
        return executeRead(entityManager -> {
            String value = keyword == null
                    ? ""
                    : keyword.trim().toLowerCase();

            StringBuilder jpql = new StringBuilder(
                    """
                    SELECT notification
                    FROM AdminNotification notification
                    LEFT JOIN FETCH notification.waitlistEntry
                    LEFT JOIN FETCH notification.room
                    WHERE notification.archived = false
                      AND (
                        notification.recipient = :recipient
                        OR notification.recipient = 'ALL'
                      )
                    """
            );

            if (!value.isBlank()) {
                jpql.append(
                        " AND LOWER(notification.message) LIKE :keyword"
                );
            }
            if (type != null) {
                jpql.append(
                        " AND notification.notificationType = :type"
                );
            }
            if (read != null) {
                jpql.append(
                        " AND notification.readFlag = :read"
                );
            }

            jpql.append(
                    " ORDER BY notification.createdAt DESC"
            );

            var query = entityManager.createQuery(
                    jpql.toString(),
                    AdminNotification.class
            );
            query.setParameter("recipient", recipient);

            if (!value.isBlank()) {
                query.setParameter("keyword", "%" + value + "%");
            }
            if (type != null) {
                query.setParameter("type", type);
            }
            if (read != null) {
                query.setParameter("read", read);
            }

            return query.getResultList();
        });
    }

    public long countUnread(String recipient) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT COUNT(notification)
                                FROM AdminNotification notification
                                WHERE notification.archived = false
                                  AND notification.readFlag = false
                                  AND (
                                    notification.recipient = :recipient
                                    OR notification.recipient = 'ALL'
                                  )
                                """,
                                Long.class
                        )
                        .setParameter("recipient", recipient)
                        .getSingleResult()
        );
    }
}
