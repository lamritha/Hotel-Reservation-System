package com.hotelreservation.util;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

public class JpaUtil {

    private static final String PERSISTENCE_UNIT_NAME = "HotelReservationPU";

    private static EntityManagerFactory entityManagerFactory;

    private static final ThreadLocal<EntityManager> TRANSACTIONAL_ENTITY_MANAGER = new ThreadLocal<>();

    private JpaUtil() {
        // Private constructor to prevent object creation
    }

    public static EntityManagerFactory getEntityManagerFactory() {
        if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
            entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        }
        return entityManagerFactory;
    }

    public static EntityManager getEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /**
     * Returns the EntityManager bound to the current {@link #executeInTransaction} call, or null.
     */
    public static EntityManager getCurrentEntityManager() {
        return TRANSACTIONAL_ENTITY_MANAGER.get();
    }

    /**
     * Runs work inside a single shared transaction so repositories can participate without
     * each opening their own EntityManager/transaction.
     */
    public static <T> T executeInTransaction(TransactionalWork<T> work) {
        EntityManager entityManager = getEntityManager();
        TRANSACTIONAL_ENTITY_MANAGER.set(entityManager);

        try {
            entityManager.getTransaction().begin();
            T result = work.execute();
            entityManager.getTransaction().commit();
            return result;

        } catch (Exception e) {
            if (entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }

            throw new RuntimeException(e);

        } finally {
            TRANSACTIONAL_ENTITY_MANAGER.remove();
            if (entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    public static void close() {
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    @FunctionalInterface
    public interface TransactionalWork<T> {
        T execute() throws Exception;
    }
}
