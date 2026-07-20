package com.hotelreservation.repository;

import com.hotelreservation.entity.Billing;

public class BillingRepository extends AbstractRepository<Billing> {

    public BillingRepository() {
        super(Billing.class);
    }

    public Billing save(Billing billing) {
        return persist(billing);
    }

    public Billing findById(Long billingId) {
        return find(billingId);
    }
}
