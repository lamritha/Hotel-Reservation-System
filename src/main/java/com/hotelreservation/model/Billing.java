package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "billings")
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "billing_id")
    private Long billingId;

    @OneToOne(optional = false)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;

    @Column(name = "subtotal", nullable = false)
    private double subtotal;

    @Column(name = "tax_amount", nullable = false)
    private double taxAmount;

    @Column(name = "total_amount", nullable = false)
    private double totalAmount;

    /*
     * Nullable for backward compatibility with Milestone 2 databases. Existing
     * rows are interpreted as having no discount.
     */
    @Column(name = "discount_percentage")
    private Double discountPercentage;

    @Column(name = "discount_amount")
    private Double discountAmount;

    @Column(name = "discount_applied_by", length = 50)
    private String discountAppliedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Billing() {
    }

    public Billing(Reservation reservation, double subtotal, double taxAmount, double totalAmount) {
        this.reservation = reservation;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.totalAmount = totalAmount;
    }

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getBillingId() {
        return billingId;
    }

    public void setBillingId(Long billingId) {
        this.billingId = billingId;
    }

    public Reservation getReservation() {
        return reservation;
    }

    public void setReservation(Reservation reservation) {
        this.reservation = reservation;
    }

    /**
     * Sums add-on charges from persisted ReservationAddOn rows (AddOn.price × quantity),
     * applying nights for PER_NIGHT pricing models. Queryable consistency check against
     * PricingService's boolean-based add-on total — not a separate billing source.
     */
    public double getPersistedAddOnTotal() {
        if (reservation == null || reservation.getReservationAddOns() == null) {
            return 0;
        }

        long nights = 0;
        if (reservation.getCheckInDate() != null && reservation.getCheckOutDate() != null) {
            nights = ChronoUnit.DAYS.between(
                    reservation.getCheckInDate(),
                    reservation.getCheckOutDate()
            );
        }

        double total = 0;
        for (ReservationAddOn reservationAddOn : reservation.getReservationAddOns()) {
            AddOn addOn = reservationAddOn.getAddOn();
            if (addOn == null) {
                continue;
            }

            double lineTotal = addOn.getPrice() * reservationAddOn.getQuantity();
            if (addOn.getPricingModel() == PricingModel.PER_NIGHT) {
                lineTotal *= nights;
            }
            total += lineTotal;
        }
        return total;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(double taxAmount) {
        this.taxAmount = taxAmount;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public double getDiscountPercentage() {
        return discountPercentage == null ? 0 : discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public double getDiscountAmount() {
        return discountAmount == null ? 0 : discountAmount;
    }

    public void setDiscountAmount(double discountAmount) {
        this.discountAmount = discountAmount;
    }

    public String getDiscountAppliedBy() {
        return discountAppliedBy;
    }

    public void setDiscountAppliedBy(String discountAppliedBy) {
        this.discountAppliedBy = discountAppliedBy;
    }

    public double getAmountAfterDiscount() {
        return Math.max(0, totalAmount - getDiscountAmount());
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
