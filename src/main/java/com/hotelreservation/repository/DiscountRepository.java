package com.hotelreservation.repository;

import com.hotelreservation.model.Discount;

import java.util.List;

public class DiscountRepository
        extends AbstractRepository<Discount> {

    public DiscountRepository() {
        super(Discount.class);
    }

    public Discount save(Discount discount) {
        return persist(discount);
    }

    public List<Discount> findByBillingId(Long billingId) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT discount
                                FROM Discount discount
                                WHERE discount.billing.billingId = :billingId
                                ORDER BY discount.appliedAt DESC
                                """,
                                Discount.class
                        )
                        .setParameter("billingId", billingId)
                        .getResultList()
        );
    }
}
