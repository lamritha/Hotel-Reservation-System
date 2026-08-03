package com.hotelreservation.service;

import com.hotelreservation.model.AdminNotification;
import com.hotelreservation.model.NotificationType;
import com.hotelreservation.repository.NotificationRepository;
import com.hotelreservation.security.AdminSession;
import com.hotelreservation.util.AppLogger;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NotificationService {

    private static final Logger LOGGER =
            AppLogger.getLogger(NotificationService.class);

    private final NotificationRepository repository;
    private final AdminSession adminSession;

    public NotificationService(
            NotificationRepository repository,
            AdminSession adminSession
    ) {
        this.repository = repository;
        this.adminSession = adminSession;
    }

    public List<AdminNotification> search(
            String keyword,
            NotificationType type,
            Boolean read
    ) {
        return repository.search(
                adminSession.getActorName(),
                keyword,
                type,
                read
        );
    }

    public void markRead(AdminNotification notification) {
        requireNotification(notification);
        notification.setRead(true);
        repository.update(notification);
        audit("NOTIFICATION_READ", notification);
    }

    public void archive(AdminNotification notification) {
        requireNotification(notification);
        notification.setArchived(true);
        repository.update(notification);
        audit("NOTIFICATION_ARCHIVED", notification);
    }

    public long countUnread() {
        return repository.countUnread(
                adminSession.getActorName()
        );
    }

    private void requireNotification(
            AdminNotification notification
    ) {
        adminSession.requireCurrentUser();
        if (notification == null) {
            throw new IllegalArgumentException(
                    "Select a notification first."
            );
        }
    }

    private void audit(
            String action,
            AdminNotification notification
    ) {
        AppLogger.audit(
                LOGGER,
                Level.INFO,
                adminSession.getActorName(),
                action,
                "AdminNotification",
                String.valueOf(
                        notification.getNotificationId()
                ),
                notification.getMessage()
        );
    }
}
