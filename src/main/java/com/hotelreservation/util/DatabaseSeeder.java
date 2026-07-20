package com.hotelreservation.util;

import com.hotelreservation.entity.AddOn;
import com.hotelreservation.entity.PricingModel;
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

            Long addOnCount = entityManager
                    .createQuery("SELECT COUNT(a) FROM AddOn a", Long.class)
                    .getSingleResult();

            if (roomCount > 0 && addOnCount > 0) {
                System.out.println("Rooms and add-ons already exist. Seeder skipped.");
                return;
            }

            entityManager.getTransaction().begin();

            if (roomCount == 0) {
                RoomFactory roomFactory = new RoomFactory();

                Room room1 = roomFactory.createRoom(RoomType.SINGLE, "101", 1);
                Room room2 = roomFactory.createRoom(RoomType.DOUBLE, "201", 2);
                Room room3 = roomFactory.createRoom(RoomType.DELUXE, "301", 3);
                Room room4 = roomFactory.createRoom(RoomType.PENTHOUSE, "501", 5);

                entityManager.persist(room1);
                entityManager.persist(room2);
                entityManager.persist(room3);
                entityManager.persist(room4);

                System.out.println("Sample rooms inserted successfully.");
                System.out.println("Single Room ID: " + room1.getRoomId());
                System.out.println("Double Room ID: " + room2.getRoomId());
                System.out.println("Deluxe Room ID: " + room3.getRoomId());
                System.out.println("Penthouse Room ID: " + room4.getRoomId());
            } else {
                System.out.println("Rooms already exist. Room seeding skipped.");
            }

            if (addOnCount == 0) {
                entityManager.persist(new AddOn("Wi-Fi", 15.00, PricingModel.PER_RESERVATION));
                entityManager.persist(new AddOn("Breakfast", 20.00, PricingModel.PER_NIGHT));
                entityManager.persist(new AddOn("Parking", 25.00, PricingModel.PER_NIGHT));
                entityManager.persist(new AddOn("Spa", 80.00, PricingModel.PER_RESERVATION));
                entityManager.persist(new AddOn("Laundry", 30.00, PricingModel.PER_RESERVATION));
                entityManager.persist(new AddOn("Airport Pickup", 60.00, PricingModel.PER_RESERVATION));

                System.out.println("Sample add-ons inserted successfully.");
            } else {
                System.out.println("Add-ons already exist. Add-on seeding skipped.");
            }

            entityManager.getTransaction().commit();

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