package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "discounts")
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Long discountId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    @Column(name = "percentage", nullable = false)
    private double percentage;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Column(name = "reason", nullable = false, length = 255)
    private String reason;

    @Column(name = "applied_by", nullable = false, length = 50)
    private String appliedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "applied_role", nullable = false, length = 20)
    private AdminRole appliedRole;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    public Discount() {
    }

    public Discount(
            Billing billing,
            double percentage,
            double amount,
            String reason,
            String appliedBy,
            AdminRole appliedRole
    ) {
        this.billing = billing;
        this.percentage = percentage;
        this.amount = amount;
        this.reason = reason;
        this.appliedBy = appliedBy;
        this.appliedRole = appliedRole;
    }

    @PrePersist
    private void onCreate() {
        if (appliedAt == null) {
            appliedAt = LocalDateTime.now();
        }
    }

    public Long getDiscountId() {
        return discountId;
    }

    public Billing getBilling() {
        return billing;
    }

    public double getPercentage() {
        return percentage;
    }

    public double getAmount() {
        return amount;
    }

    public String getReason() {
        return reason;
    }

    public String getAppliedBy() {
        return appliedBy;
    }

    public AdminRole getAppliedRole() {
        return appliedRole;
    }

    public LocalDateTime getAppliedAt() {
        return appliedAt;
    }
}
