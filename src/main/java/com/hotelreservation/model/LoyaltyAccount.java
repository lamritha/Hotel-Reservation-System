package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "loyalty_accounts")
public class LoyaltyAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "loyalty_id")
    private Long loyaltyID;

    @Column(name = "loyalty_number", nullable = false, unique = true, length = 30)
    private String loyaltyNumber;

    @Column(name = "points_balance", nullable = false)
    private int pointsBalance;

    @Column(name = "total_earned", nullable = false)
    private int totalEarned;

    @Column(name = "total_redeemed", nullable = false)
    private int totalRedeemed;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "guest_id", nullable = false, unique = true)
    private Guest guest;

    public LoyaltyAccount() {
    }

    public LoyaltyAccount(
            Guest guest,
            String loyaltyNumber,
            int pointsBalance,
            int totalEarned,
            int totalRedeemed
    ) {
        this.guest = guest;
        this.loyaltyNumber = loyaltyNumber;
        this.pointsBalance = pointsBalance;
        this.totalEarned = totalEarned;
        this.totalRedeemed = totalRedeemed;
    }

    @PrePersist
    private void onCreate() {
        if (enrolledAt == null) {
            enrolledAt = LocalDateTime.now();
        }
    }

    public Long getLoyaltyID() {
        return loyaltyID;
    }

    public void setLoyaltyID(Long loyaltyID) {
        this.loyaltyID = loyaltyID;
    }

    public String getLoyaltyNumber() {
        return loyaltyNumber;
    }

    public void setLoyaltyNumber(String loyaltyNumber) {
        this.loyaltyNumber = loyaltyNumber;
    }

    public int getPointsBalance() {
        return pointsBalance;
    }

    public void setPointsBalance(int pointsBalance) {
        this.pointsBalance = pointsBalance;
    }

    public int getTotalEarned() {
        return totalEarned;
    }

    public void setTotalEarned(int totalEarned) {
        this.totalEarned = totalEarned;
    }

    public int getTotalRedeemed() {
        return totalRedeemed;
    }

    public void setTotalRedeemed(int totalRedeemed) {
        this.totalRedeemed = totalRedeemed;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }
}
