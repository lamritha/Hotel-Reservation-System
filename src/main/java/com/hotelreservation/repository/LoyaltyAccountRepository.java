package com.hotelreservation.repository;

import com.hotelreservation.model.LoyaltyAccount;

import java.util.List;
import java.util.Optional;

public class LoyaltyAccountRepository extends AbstractRepository<LoyaltyAccount> {

    public LoyaltyAccountRepository() {
        super(LoyaltyAccount.class);
    }

    public LoyaltyAccount save(LoyaltyAccount loyaltyAccount) {
        return persist(loyaltyAccount);
    }

    public LoyaltyAccount update(
            LoyaltyAccount loyaltyAccount
    ) {
        return merge(loyaltyAccount);
    }

    public Optional<LoyaltyAccount> findByGuestId(
            Long guestId
    ) {
        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            """
                            SELECT account
                            FROM LoyaltyAccount account
                            JOIN FETCH account.guest
                            WHERE account.guest.guestId = :guestId
                            """,
                            LoyaltyAccount.class
                    )
                    .setParameter("guestId", guestId)
                    .getResultList();

            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public Optional<LoyaltyAccount> findByLoyaltyNumber(
            String loyaltyNumber
    ) {
        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            """
                            SELECT account
                            FROM LoyaltyAccount account
                            JOIN FETCH account.guest
                            WHERE LOWER(account.loyaltyNumber)
                                = LOWER(:loyaltyNumber)
                            """,
                            LoyaltyAccount.class
                    )
                    .setParameter(
                            "loyaltyNumber",
                            loyaltyNumber == null
                                    ? ""
                                    : loyaltyNumber.trim()
                    )
                    .getResultList();

            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public Optional<LoyaltyAccount> findByGuestPhone(String phone) {
        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            """
                            SELECT account
                            FROM LoyaltyAccount account
                            JOIN FETCH account.guest guest
                            WHERE guest.phone = :phone
                            """,
                            LoyaltyAccount.class
                    )
                    .setParameter("phone", phone)
                    .getResultList();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        });
    }

    public List<LoyaltyAccount> search(String keyword) {
        return executeRead(entityManager -> {
            String value = keyword == null
                    ? ""
                    : keyword.trim().toLowerCase();

            String jpql = """
                    SELECT account
                    FROM LoyaltyAccount account
                    JOIN FETCH account.guest guest
                    WHERE :keyword = ''
                       OR LOWER(account.loyaltyNumber) LIKE :pattern
                       OR LOWER(guest.firstName) LIKE :pattern
                       OR LOWER(guest.lastName) LIKE :pattern
                       OR LOWER(guest.email) LIKE :pattern
                       OR LOWER(guest.phone) LIKE :pattern
                    ORDER BY account.enrolledAt DESC
                    """;

            return entityManager.createQuery(
                            jpql,
                            LoyaltyAccount.class
                    )
                    .setParameter("keyword", value)
                    .setParameter("pattern", "%" + value + "%")
                    .getResultList();
        });
    }
}
