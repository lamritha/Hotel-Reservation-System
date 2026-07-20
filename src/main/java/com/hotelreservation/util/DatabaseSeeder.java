package com.hotelreservation.util;

import com.hotelreservation.entity.Room;
import com.hotelreservation.entity.RoomType;
import com.hotelreservation.factory.RoomFactory;
import jakarta.persistence.EntityManager;

public class DatabaseSeeder {

    public static void main(String[] args) {
        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();

            Long roomCount = entityManager
                    .createQuery("SELECT COUNT(r) FROM Room r", Long.class)
                    .getSingleResult();

            if (roomCount > 0) {
                System.out.println("Rooms already exist. Seeder skipped.");
                return;
            }

            RoomFactory roomFactory = new RoomFactory();

            Room room1 = roomFactory.createRoom(RoomType.SINGLE, "101", 1);
            Room room2 = roomFactory.createRoom(RoomType.DOUBLE, "201", 2);
            Room room3 = roomFactory.createRoom(RoomType.DELUXE, "301", 3);
            Room room4 = roomFactory.createRoom(RoomType.PENTHOUSE, "501", 5);

            entityManager.getTransaction().begin();

            entityManager.persist(room1);
            entityManager.persist(room2);
            entityManager.persist(room3);
            entityManager.persist(room4);

            entityManager.getTransaction().commit();

            System.out.println("Sample rooms inserted successfully.");
            System.out.println("Single Room ID: " + room1.getRoomId());
            System.out.println("Double Room ID: " + room2.getRoomId());
            System.out.println("Deluxe Room ID: " + room3.getRoomId());
            System.out.println("Penthouse Room ID: " + room4.getRoomId());

        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            System.out.println("Database seeding failed.");
            e.printStackTrace();

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }

            JpaUtil.close();
        }
    }
}