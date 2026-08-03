package com.hotelreservation.repository;

import com.hotelreservation.model.Guest;

import java.util.List;
import java.util.Optional;

public class GuestRepository extends AbstractRepository<Guest> {

    public GuestRepository() {
        super(Guest.class);
    }

    public Guest save(Guest guest) {
        return persist(guest);
    }

    public Guest update(Guest guest) {
        return merge(guest);
    }

    public Guest findById(Long guestId) {
        return find(guestId);
    }

    public Optional<Guest> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            """
                            SELECT guest
                            FROM Guest guest
                            WHERE LOWER(guest.email) = LOWER(:email)
                            """,
                            Guest.class
                    )
                    .setParameter("email", email.trim())
                    .getResultList();

            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public List<Guest> search(String keyword) {
        return executeRead(entityManager -> {
            String value = keyword == null
                    ? ""
                    : keyword.trim().toLowerCase();

            return entityManager.createQuery(
                            """
                            SELECT guest
                            FROM Guest guest
                            WHERE :keyword = ''
                               OR LOWER(guest.firstName) LIKE :pattern
                               OR LOWER(guest.lastName) LIKE :pattern
                               OR LOWER(guest.email) LIKE :pattern
                               OR LOWER(guest.phone) LIKE :pattern
                            ORDER BY guest.createdAt DESC
                            """,
                            Guest.class
                    )
                    .setParameter("keyword", value)
                    .setParameter("pattern", "%" + value + "%")
                    .getResultList();
        });
    }

    public long countAll() {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                "SELECT COUNT(guest) FROM Guest guest",
                                Long.class
                        )
                        .getSingleResult()
        );
    }
}
