package com.hotelreservation.repository;

import com.hotelreservation.model.LoyaltyTransaction;
import com.hotelreservation.model.LoyaltyTransactionType;

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

    public double sumRedeemedAmountByReservationId(
            Long reservationId
    ) {
        if (reservationId == null) {
            return 0;
        }

        return executeRead(entityManager -> {
            Double result = entityManager.createQuery(
                            """
                            SELECT SUM(transaction.monetaryAmount)
                            FROM LoyaltyTransaction transaction
                            WHERE transaction.reservation.reservationId
                                = :reservationId
                              AND transaction.transactionType = :type
                            """,
                            Double.class
                    )
                    .setParameter(
                            "reservationId",
                            reservationId
                    )
                    .setParameter(
                            "type",
                            LoyaltyTransactionType.REDEEM
                    )
                    .getSingleResult();

            return result == null ? 0 : result;
        });
    }
}
