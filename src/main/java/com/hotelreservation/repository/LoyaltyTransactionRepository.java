package com.hotelreservation.repository;

import com.hotelreservation.model.LoyaltyTransaction;

import java.util.List;

public class LoyaltyTransactionRepository
        extends AbstractRepository<LoyaltyTransaction> {

    public LoyaltyTransactionRepository() {
        super(LoyaltyTransaction.class);
    }

    public LoyaltyTransaction save(
            LoyaltyTransaction transaction
    ) {
        return persist(transaction);
    }

    public List<LoyaltyTransaction> findByAccountId(
            Long accountId
    ) {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                """
                                SELECT transaction
                                FROM LoyaltyTransaction transaction
                                LEFT JOIN FETCH transaction.reservation
                                WHERE transaction.loyaltyAccount.loyaltyID
                                    = :accountId
                                ORDER BY transaction.createdAt DESC
                                """,
                                LoyaltyTransaction.class
                        )
                        .setParameter("accountId", accountId)
                        .getResultList()
        );
    }
}
