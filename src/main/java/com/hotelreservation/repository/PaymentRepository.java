package com.hotelreservation.repository;

import com.hotelreservation.entity.Payment;

public class PaymentRepository extends AbstractRepository<Payment> {

    public PaymentRepository() {
        super(Payment.class);
    }

    public Payment save(Payment payment) {
        return persist(payment);
    }

    public Payment findById(Long paymentId) {
        return find(paymentId);
    }
}
