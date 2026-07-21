package com.hotelreservation.util;

import com.hotelreservation.model.AddOn;
import com.hotelreservation.model.Guest;
import com.hotelreservation.model.LoyaltyAccount;
import com.hotelreservation.model.PricingModel;
import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.factory.RoomFactory;
import jakarta.persistence.EntityManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DatabaseSeeder {

    private static final int TARGET_ROOMS_PER_TYPE = 3;
    private static final int TARGET_LOYALTY_ACCOUNTS = 2;

    public static void main(String[] args) {
        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();

            Long addOnCount = entityManager
                    .createQuery("SELECT COUNT(a) FROM AddOn a", Long.class)
                    .getSingleResult();

            entityManager.getTransaction().begin();

            RoomFactory roomFactory = new RoomFactory();
            ensureRoomsForType(entityManager, roomFactory, RoomType.SINGLE, 1, 101);
            ensureRoomsForType(entityManager, roomFactory, RoomType.DOUBLE, 2, 201);
            ensureRoomsForType(entityManager, roomFactory, RoomType.DELUXE, 3, 301);
            ensureRoomsForType(entityManager, roomFactory, RoomType.PENTHOUSE, 5, 501);

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

            ensureLoyaltyAccounts(entityManager);

            // Availability is date-overlap based; clear stale OCCUPIED flags from testing.
            int resetCount = entityManager
                    .createQuery("UPDATE Room r SET r.status = :status")
                    .setParameter("status", RoomStatus.AVAILABLE)
                    .executeUpdate();
            System.out.println("Reset " + resetCount + " room(s) to AVAILABLE.");

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

    private static void ensureRoomsForType(
            EntityManager entityManager,
            RoomFactory roomFactory,
            RoomType roomType,
            int floor,
            int startingRoomNumber
    ) {
        Long existingCount = entityManager
                .createQuery(
                        "SELECT COUNT(r) FROM Room r WHERE r.roomType = :type",
                        Long.class
                )
                .setParameter("type", roomType)
                .getSingleResult();

        if (existingCount >= TARGET_ROOMS_PER_TYPE) {
            System.out.println(roomType + " already has " + existingCount
                    + " room(s). Seeding skipped.");
            return;
        }

        int needed = (int) (TARGET_ROOMS_PER_TYPE - existingCount);

        List<String> existingNumbers = entityManager
                .createQuery(
                        "SELECT r.roomNumber FROM Room r WHERE r.roomType = :type",
                        String.class
                )
                .setParameter("type", roomType)
                .getResultList();

        Set<String> usedNumbers = new HashSet<>(existingNumbers);

        int nextNumber = startingRoomNumber;
        int inserted = 0;

        while (inserted < needed) {
            String roomNumber = String.valueOf(nextNumber);
            nextNumber++;

            if (usedNumbers.contains(roomNumber)) {
                continue;
            }

            Room room = roomFactory.createRoom(roomType, roomNumber, floor);
            entityManager.persist(room);
            usedNumbers.add(roomNumber);
            inserted++;
            System.out.println(roomType + " room " + roomNumber + " inserted.");
        }

        System.out.println(roomType + ": inserted " + inserted
                + " room(s) to reach " + TARGET_ROOMS_PER_TYPE + ".");
    }

    /**
     * Seeds up to two loyalty accounts with known phones for kiosk testing.
     * Creates dedicated guests when needed so lookup-by-phone works without H2 console.
     */
    private static void ensureLoyaltyAccounts(EntityManager entityManager) {
        Long loyaltyCount = entityManager
                .createQuery("SELECT COUNT(la) FROM LoyaltyAccount la", Long.class)
                .getSingleResult();

        if (loyaltyCount >= TARGET_LOYALTY_ACCOUNTS) {
            System.out.println("Loyalty accounts already exist (" + loyaltyCount
                    + "). Loyalty seeding skipped.");
            return;
        }

        int needed = (int) (TARGET_LOYALTY_ACCOUNTS - loyaltyCount);

        if (needed >= 1) {
            Guest guest1 = findOrCreateSeedGuest(
                    entityManager,
                    "loyalty.guest1@hotel.test",
                    "416-555-0198",
                    "Loyalty",
                    "GuestOne"
            );
            entityManager.persist(new LoyaltyAccount(guest1, "LOY-1001", 420, 500, 80));
            System.out.println("Loyalty account LOY-1001 seeded for phone 416-555-0198.");
        }

        if (needed >= 2) {
            Guest guest2 = findOrCreateSeedGuest(
                    entityManager,
                    "loyalty.guest2@hotel.test",
                    "416-555-0100",
                    "Loyalty",
                    "GuestTwo"
            );
            entityManager.persist(new LoyaltyAccount(guest2, "LOY-1002", 150, 150, 0));
            System.out.println("Loyalty account LOY-1002 seeded for phone 416-555-0100.");
        }
    }

    private static Guest findOrCreateSeedGuest(
            EntityManager entityManager,
            String email,
            String phone,
            String firstName,
            String lastName
    ) {
        List<Guest> matches = entityManager
                .createQuery("SELECT g FROM Guest g WHERE g.email = :email", Guest.class)
                .setParameter("email", email)
                .getResultList();

        if (!matches.isEmpty()) {
            Guest existing = matches.getFirst();
            existing.setPhone(phone);
            return existing;
        }

        Guest guest = new Guest(firstName, lastName, email, phone, "123 Loyalty Seed St");
        entityManager.persist(guest);
        return guest;
    }
}
