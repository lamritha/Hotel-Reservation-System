package com.hotelreservation.repository;

import com.hotelreservation.model.LoyaltyAccount;

import java.util.Optional;

public class LoyaltyAccountRepository extends AbstractRepository<LoyaltyAccount> {

    public LoyaltyAccountRepository() {
        super(LoyaltyAccount.class);
    }

    public LoyaltyAccount save(LoyaltyAccount loyaltyAccount) {
        return persist(loyaltyAccount);
    }

    public Optional<LoyaltyAccount> findByGuestPhone(String phone) {
        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            """
                            SELECT la FROM LoyaltyAccount la
                            JOIN la.guest g
                            WHERE g.phone = :phone
                            """,
                            LoyaltyAccount.class
                    )
                    .setParameter("phone", phone)
                    .getResultList();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        });
    }
}
