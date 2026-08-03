package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_transactions")
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loyalty_transaction_id")
    private Long loyaltyTransactionId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "loyalty_id", nullable = false)
    private LoyaltyAccount loyaltyAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id")
    private Reservation reservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private LoyaltyTransactionType transactionType;

    @Column(name = "points_change", nullable = false)
    private int pointsChange;

    @Column(name = "monetary_amount", nullable = false)
    private double monetaryAmount;

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public LoyaltyTransaction() {
    }

    public LoyaltyTransaction(
            LoyaltyAccount loyaltyAccount,
            Reservation reservation,
            LoyaltyTransactionType transactionType,
            int pointsChange,
            double monetaryAmount,
            String description
    ) {
        this.loyaltyAccount = loyaltyAccount;
        this.reservation = reservation;
        this.transactionType = transactionType;
        this.pointsChange = pointsChange;
        this.monetaryAmount = monetaryAmount;
        this.description = description;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getLoyaltyTransactionId() {
        return loyaltyTransactionId;
    }

    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public LoyaltyTransactionType getTransactionType() {
        return transactionType;
    }

    public int getPointsChange() {
        return pointsChange;
    }

    public double getMonetaryAmount() {
        return monetaryAmount;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
