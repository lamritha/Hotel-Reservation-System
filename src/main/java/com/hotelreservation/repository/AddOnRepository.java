package com.hotelreservation.repository;

import com.hotelreservation.model.AddOn;

import java.util.Optional;

public class AddOnRepository extends AbstractRepository<AddOn> {

    public AddOnRepository() {
        super(AddOn.class);
    }

    public AddOn save(AddOn addOn) {
        return persist(addOn);
    }

    public Optional<AddOn> findByName(String name) {
        return executeRead(entityManager -> {
            var results = entityManager.createQuery(
                            "SELECT a FROM AddOn a WHERE a.name = :name",
                            AddOn.class
                    )
                    .setParameter("name", name)
                    .getResultList();

            return results.isEmpty() ? Optional.empty() : Optional.of(results.getFirst());
        });
    }
}
