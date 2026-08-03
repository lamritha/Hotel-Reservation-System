package com.hotelreservation.repository;

import com.hotelreservation.model.Payment;

import java.time.LocalDate;
import java.util.List;

public class PaymentRepository
        extends AbstractRepository<Payment> {

    public PaymentRepository() {
        super(Payment.class);
    }

    public Payment save(Payment payment) {
        return persist(payment);
    }

    public List<Payment> findByBillingId(Long billingId) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT payment
                                FROM Payment payment
                                WHERE payment.billing.billingId = :billingId
                                ORDER BY payment.paymentDate DESC
                                """,
                                Payment.class
                        )
                        .setParameter("billingId", billingId)
                        .getResultList()
        );
    }

    public double getNetPaid(Long billingId) {
        return executeRead(entityManager -> {
            Double result = entityManager.createQuery(
                            """
                            SELECT SUM(payment.amount)
                            FROM Payment payment
                            WHERE payment.billing.billingId = :billingId
                            """,
                            Double.class
                    )
                    .setParameter("billingId", billingId)
                    .getSingleResult();

            return result == null ? 0 : result;
        });
    }

    public double sumPaymentsForDate(LocalDate date) {
        return executeRead(entityManager -> {
            Double result = entityManager.createQuery(
                            """
                            SELECT SUM(payment.amount)
                            FROM Payment payment
                            WHERE payment.paymentDate >= :from
                              AND payment.paymentDate < :to
                            """,
                            Double.class
                    )
                    .setParameter("from", date.atStartOfDay())
                    .setParameter(
                            "to",
                            date.plusDays(1).atStartOfDay()
                    )
                    .getSingleResult();
            return result == null ? 0 : result;
        });
    }
}
