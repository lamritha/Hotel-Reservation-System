package com.hotelreservation.repository;

import com.hotelreservation.model.AdminUser;

import java.util.List;
import java.util.Optional;

public class AdminUserRepository extends AbstractRepository<AdminUser> {

    public AdminUserRepository() {
        super(AdminUser.class);
    }

    public AdminUser save(AdminUser adminUser) {
        return persist(adminUser);
    }

    public AdminUser update(AdminUser adminUser) {
        return merge(adminUser);
    }

    public AdminUser findById(Long adminId) {
        return find(adminId);
    }

    public Optional<AdminUser> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        return executeRead(entityManager -> {
            List<AdminUser> results = entityManager.createQuery(
                            """
                            SELECT a FROM AdminUser a
                            WHERE LOWER(a.username) = LOWER(:username)
                            """,
                            AdminUser.class
                    )
                    .setParameter("username", username.trim())
                    .getResultList();

            return results.isEmpty()
                    ? Optional.empty()
                    : Optional.of(results.getFirst());
        });
    }

    public List<AdminUser> findAll() {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                "SELECT a FROM AdminUser a ORDER BY a.username",
                                AdminUser.class
                        )
                        .getResultList()
        );
    }

    public long count() {
        return executeRead(entityManager ->
                entityManager.createQuery(
                                "SELECT COUNT(a) FROM AdminUser a",
                                Long.class
                        )
                        .getSingleResult()
        );
    }
}