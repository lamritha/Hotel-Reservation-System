package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "admin_notifications")
public class AdminNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    @Column(name = "recipient", nullable = false, length = 50)
    private String recipient;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waitlist_id")
    private WaitlistEntry waitlistEntry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(name = "is_read", nullable = false)
    private boolean readFlag;

    @Column(name = "is_archived", nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AdminNotification() {
    }

    public AdminNotification(
            NotificationType notificationType,
            String recipient,
            String message,
            WaitlistEntry waitlistEntry,
            Room room
    ) {
        this.notificationType = notificationType;
        this.recipient = recipient;
        this.message = message;
        this.waitlistEntry = waitlistEntry;
        this.room = room;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public NotificationType getNotificationType() {
        return notificationType;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getMessage() {
        return message;
    }

    public WaitlistEntry getWaitlistEntry() {
        return waitlistEntry;
    }

    public Room getRoom() {
        return room;
    }

    public boolean isRead() {
        return readFlag;
    }

    public void setRead(boolean read) {
        this.readFlag = read;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
