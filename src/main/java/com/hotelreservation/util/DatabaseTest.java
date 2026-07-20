package com.hotelreservation.util;

import jakarta.persistence.EntityManager;

public class DatabaseTest {

    public static void main(String[] args) {
        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();

            System.out.println("Database connection successful.");
            System.out.println("EntityManager is open: " + entityManager.isOpen());

        } catch (Exception e) {
            System.out.println("Database connection failed.");
            e.printStackTrace();

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }

            JpaUtil.close();
        }
    }
}