package com.hotelreservation.app;

import com.hotelreservation.config.HotelPolicyConfig;
import com.hotelreservation.factory.RoomFactory;
import com.hotelreservation.model.AddOn;
import com.hotelreservation.model.PricingModel;
import com.hotelreservation.model.RoomType;
import com.hotelreservation.util.AppLogger;
import com.hotelreservation.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Idempotently prepares rooms and add-ons required by a fresh installation.
 */
public class ApplicationDataSeeder {

    private static final Logger LOGGER =
            AppLogger.getLogger(
                    ApplicationDataSeeder.class
            );
    private static final int ROOMS_PER_TYPE = 3;

    public void seedDefaults() {
        JpaUtil.executeInTransaction(() -> {
            EntityManager entityManager =
                    JpaUtil.getCurrentEntityManager();
            seedRooms(entityManager);
            seedAddOns(entityManager);
            return null;
        });

        AppLogger.audit(
                LOGGER,
                Level.INFO,
                "SYSTEM",
                "REFERENCE_DATA_READY",
                "Database",
                "-",
                "Default rooms and add-ons are ready."
        );
    }

    private void seedRooms(EntityManager entityManager) {
        RoomFactory factory = new RoomFactory();
        seedRoomType(
                entityManager,
                factory,
                RoomType.SINGLE,
                1,
                101
        );
        seedRoomType(
                entityManager,
                factory,
                RoomType.DOUBLE,
                2,
                201
        );
        seedRoomType(
                entityManager,
                factory,
                RoomType.DELUXE,
                3,
                301
        );
        seedRoomType(
                entityManager,
                factory,
                RoomType.PENTHOUSE,
                5,
                501
        );
    }

    private void seedRoomType(
            EntityManager entityManager,
            RoomFactory factory,
            RoomType type,
            int floor,
            int firstRoomNumber
    ) {
        Long existingCount = entityManager.createQuery(
                        """
                        SELECT COUNT(room)
                        FROM Room room
                        WHERE room.roomType = :type
                        """,
                        Long.class
                )
                .setParameter("type", type)
                .getSingleResult();
        Set<String> usedRoomNumbers = new HashSet<>(
                entityManager.createQuery(
                                """
                                SELECT room.roomNumber
                                FROM Room room
                                """,
                                String.class
                        )
                        .getResultList()
        );

        int candidate = firstRoomNumber;
        while (existingCount < ROOMS_PER_TYPE) {
            String roomNumber = String.valueOf(candidate++);
            if (usedRoomNumbers.contains(roomNumber)) {
                continue;
            }
            entityManager.persist(
                    factory.createRoom(
                            type,
                            roomNumber,
                            floor
                    )
            );
            usedRoomNumbers.add(roomNumber);
            existingCount++;
        }
    }

    private void seedAddOns(EntityManager entityManager) {
        seedAddOn(
                entityManager,
                "Wi-Fi",
                HotelPolicyConfig.WIFI_PRICE,
                PricingModel.PER_RESERVATION
        );
        seedAddOn(
                entityManager,
                "Breakfast",
                HotelPolicyConfig.BREAKFAST_PRICE_PER_NIGHT,
                PricingModel.PER_NIGHT
        );
        seedAddOn(
                entityManager,
                "Parking",
                HotelPolicyConfig.PARKING_PRICE_PER_NIGHT,
                PricingModel.PER_NIGHT
        );
        seedAddOn(
                entityManager,
                "Spa",
                HotelPolicyConfig.SPA_PRICE,
                PricingModel.PER_RESERVATION
        );
        seedAddOn(
                entityManager,
                "Laundry",
                HotelPolicyConfig.LAUNDRY_PRICE,
                PricingModel.PER_RESERVATION
        );
        seedAddOn(
                entityManager,
                "Airport Pickup",
                HotelPolicyConfig.AIRPORT_PICKUP_PRICE,
                PricingModel.PER_RESERVATION
        );
    }

    private void seedAddOn(
            EntityManager entityManager,
            String name,
            double price,
            PricingModel model
    ) {
        Long count = entityManager.createQuery(
                        """
                        SELECT COUNT(addOn)
                        FROM AddOn addOn
                        WHERE LOWER(addOn.name) = LOWER(:name)
                        """,
                        Long.class
                )
                .setParameter("name", name)
                .getSingleResult();
        if (count == 0) {
            entityManager.persist(
                    new AddOn(name, price, model)
            );
        }
    }
}
