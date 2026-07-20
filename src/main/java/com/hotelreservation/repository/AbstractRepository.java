package com.hotelreservation.repository;

import com.hotelreservation.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.function.Function;

/**
 * Shared persistence helpers for entity repositories.
 * Joins an active shared transaction when present; otherwise opens a local one.
 */
abstract class AbstractRepository<T> {

    private final Class<T> entityClass;
    private final String entityLabel;

    protected AbstractRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.entityLabel = entityClass.getSimpleName().toLowerCase();
    }

    protected T persist(T entity) {
        EntityManager sharedEntityManager = JpaUtil.getCurrentEntityManager();

        if (sharedEntityManager != null) {
            sharedEntityManager.persist(entity);
            return entity;
        }

        return executeWrite(
                entityManager -> {
                    entityManager.persist(entity);
                    return entity;
                },
                "Failed to save " + entityLabel
        );
    }

    protected T merge(T entity) {
        EntityManager sharedEntityManager = JpaUtil.getCurrentEntityManager();

        if (sharedEntityManager != null) {
            return sharedEntityManager.merge(entity);
        }

        return executeWrite(
                entityManager -> entityManager.merge(entity),
                "Failed to update " + entityLabel
        );
    }

    protected T find(Long id) {
        return executeRead(entityManager -> entityManager.find(entityClass, id));
    }

    protected <R> R executeRead(Function<EntityManager, R> work) {
        EntityManager sharedEntityManager = JpaUtil.getCurrentEntityManager();

        if (sharedEntityManager != null) {
            return work.apply(sharedEntityManager);
        }

        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();
            return work.apply(entityManager);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private <R> R executeWrite(Function<EntityManager, R> work, String errorMessage) {
        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();
            entityManager.getTransaction().begin();

            R result = work.apply(entityManager);

            entityManager.getTransaction().commit();
            return result;

        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            throw new RuntimeException(errorMessage, e);

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
