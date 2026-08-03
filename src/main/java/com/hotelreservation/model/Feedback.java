package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "feedback",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_feedback_reservation",
                columnNames = "reservation_id"
        )
)
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feedback_id")
    private Long feedbackId;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "comment", nullable = false, length = 1000)
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_tag", nullable = false, length = 30)
    private SentimentTag sentimentTag;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public Feedback() {
    }

    public Feedback(
            Reservation reservation,
            Guest guest,
            int rating,
            String comment,
            SentimentTag sentimentTag
    ) {
        this.reservation = reservation;
        this.guest = guest;
        this.rating = rating;
        this.comment = comment;
        this.sentimentTag = sentimentTag;
    }

    @PrePersist
    private void onCreate() {
        if (submittedAt == null) {
            submittedAt = LocalDateTime.now();
        }
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public Guest getGuest() {
        return guest;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public SentimentTag getSentimentTag() {
        return sentimentTag;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
}
