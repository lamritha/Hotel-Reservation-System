package com.hotelreservation.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_id", nullable = false)
    private Billing billing;

    @Column(name = "amount", nullable = false)
    private double amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    /*
     * Nullable so existing Milestone 2 payment rows remain compatible. A null
     * value is treated as PARTIAL_PAYMENT.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", length = 30)
    private PaymentType paymentType;

    @Column(name = "payment_date", nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "note", length = 255)
    private String note;

    public Payment() {
        this.paymentDate = LocalDateTime.now();
    }

    public Payment(Billing billing, double amount, PaymentMethod paymentMethod, String note) {
        this(
                billing,
                amount,
                paymentMethod,
                PaymentType.PARTIAL_PAYMENT,
                note
        );
    }

    public Payment(
            Billing billing,
            double amount,
            PaymentMethod paymentMethod,
            PaymentType paymentType,
            String note
    ) {
        this.billing = billing;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentType = paymentType;
        this.note = note;
        this.paymentDate = LocalDateTime.now();
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public Billing getBilling() {
        return billing;
    }

    public void setBilling(Billing billing) {
        this.billing = billing;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public PaymentType getPaymentType() {
        return paymentType == null
                ? PaymentType.PARTIAL_PAYMENT
                : paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDateTime paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
