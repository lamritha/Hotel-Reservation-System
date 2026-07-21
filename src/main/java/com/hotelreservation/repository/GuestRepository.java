package com.hotelreservation.repository;

import com.hotelreservation.model.Guest;

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
        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            "SELECT g FROM Guest g WHERE g.email = :email",
                            Guest.class
                    )
                    .setParameter("email", email)
                    .getResultList();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        });
    }
}
