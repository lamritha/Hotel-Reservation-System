package com.hotelreservation.util;

import com.hotelreservation.entity.*;
import com.hotelreservation.factory.RoomFactory;
import com.hotelreservation.service.BookingService;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

/**
 * One-shot verification: complete a booking with add-ons and print persisted rows.
 */
public class VerifyAddOnBooking {

    public static void main(String[] args) {
        EntityManager entityManager = null;

        try {
            seedAddOnsAndRoomsIfNeeded();
            ensureAvailableRoom(RoomType.SINGLE);

            BookingSession.reset();
            BookingSession.setFirstName("Verify");
            BookingSession.setLastName("Guest");
            BookingSession.setEmail("verify" + System.currentTimeMillis() + "@email.com");
            BookingSession.setPhone("416-555-0199");
            BookingSession.setAddress("1 Verify St");
            BookingSession.setNumAdults(1);
            BookingSession.setNumChildren(0);
            BookingSession.setCheckInDate(LocalDate.of(2026, 8, 1));
            BookingSession.setCheckOutDate(LocalDate.of(2026, 8, 4)); // 3 nights
            BookingSession.setSelectedRoomType(RoomType.SINGLE);
            BookingSession.setSingleRoomQuantity(1);
            BookingSession.setWifiSelected(true);
            BookingSession.setBreakfastSelected(true); // PER_NIGHT → 20 * 3 = 60

            Guest guest = new Guest(
                    BookingSession.getFirstName(),
                    BookingSession.getLastName(),
                    BookingSession.getEmail(),
                    BookingSession.getPhone(),
                    BookingSession.getAddress()
            );

            BookingService bookingService = new BookingService();
            Reservation reservation = bookingService.completeBooking(
                    guest,
                    RoomType.SINGLE,
                    BookingSession.getCheckInDate(),
                    BookingSession.getCheckOutDate(),
                    1,
                    0,
                    false,
                    PaymentMethod.CARD,
                    true
            );

            Long reservationId = reservation.getReservationId();
            System.out.println("=== VERIFY BOOKING ID: " + reservationId + " ===");

            entityManager = JpaUtil.getEntityManager();

            List<ReservationAddOn> addOns = entityManager.createQuery(
                            "SELECT ra FROM ReservationAddOn ra "
                                    + "JOIN FETCH ra.addOn "
                                    + "WHERE ra.reservation.reservationId = :rid",
                            ReservationAddOn.class
                    )
                    .setParameter("rid", reservationId)
                    .getResultList();

            System.out.println("--- ReservationAddOn rows ---");
            for (ReservationAddOn ra : addOns) {
                System.out.println(String.format(
                        "reservation_id=%d | addon_id=%d | name=%s | unit_price=%.2f | pricing_model=%s | quantity=%d | line=(price*qty%s)",
                        ra.getId().getReservationID(),
                        ra.getId().getAddonID(),
                        ra.getAddOn().getName(),
                        ra.getAddOn().getPrice(),
                        ra.getAddOn().getPricingModel(),
                        ra.getQuantity(),
                        ra.getAddOn().getPricingModel() == PricingModel.PER_NIGHT ? "*nights" : ""
                ));
            }

            Billing billing = entityManager.createQuery(
                            "SELECT b FROM Billing b "
                                    + "JOIN FETCH b.reservation r "
                                    + "LEFT JOIN FETCH r.reservationAddOns ra "
                                    + "LEFT JOIN FETCH ra.addOn "
                                    + "WHERE r.reservationId = :rid",
                            Billing.class
                    )
                    .setParameter("rid", reservationId)
                    .getSingleResult();

            System.out.println("--- Billing row ---");
            System.out.println(String.format(
                    "billing_id=%d | reservation_id=%d | subtotal=%.2f | tax_amount=%.2f | total_amount=%.2f | persistedAddOnTotal=%.2f",
                    billing.getBillingId(),
                    billing.getReservation().getReservationId(),
                    billing.getSubtotal(),
                    billing.getTaxAmount(),
                    billing.getTotalAmount(),
                    billing.getPersistedAddOnTotal()
            ));

        } catch (Exception e) {
            System.out.println("VerifyAddOnBooking failed.");
            e.printStackTrace();
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
            JpaUtil.close();
        }
    }

    private static void seedAddOnsAndRoomsIfNeeded() {
        EntityManager entityManager = null;
        try {
            entityManager = JpaUtil.getEntityManager();
            Long addOnCount = entityManager
                    .createQuery("SELECT COUNT(a) FROM AddOn a", Long.class)
                    .getSingleResult();
            Long roomCount = entityManager
                    .createQuery("SELECT COUNT(r) FROM Room r", Long.class)
                    .getSingleResult();

            if (addOnCount > 0 && roomCount > 0) {
                System.out.println("Seed data already present.");
                return;
            }

            entityManager.getTransaction().begin();
            if (roomCount == 0) {
                RoomFactory roomFactory = new RoomFactory();
                entityManager.persist(roomFactory.createRoom(RoomType.SINGLE, "101", 1));
                entityManager.persist(roomFactory.createRoom(RoomType.DOUBLE, "201", 2));
                entityManager.persist(roomFactory.createRoom(RoomType.DELUXE, "301", 3));
                entityManager.persist(roomFactory.createRoom(RoomType.PENTHOUSE, "501", 5));
            }
            if (addOnCount == 0) {
                entityManager.persist(new AddOn("Wi-Fi", 15.00, PricingModel.PER_RESERVATION));
                entityManager.persist(new AddOn("Breakfast", 20.00, PricingModel.PER_NIGHT));
                entityManager.persist(new AddOn("Parking", 25.00, PricingModel.PER_NIGHT));
                entityManager.persist(new AddOn("Spa", 80.00, PricingModel.PER_RESERVATION));
                entityManager.persist(new AddOn("Laundry", 30.00, PricingModel.PER_RESERVATION));
                entityManager.persist(new AddOn("Airport Pickup", 60.00, PricingModel.PER_RESERVATION));
            }
            entityManager.getTransaction().commit();
            System.out.println("Seeded rooms/add-ons for verify run.");
        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }

    private static void ensureAvailableRoom(RoomType roomType) {
        EntityManager entityManager = null;
        try {
            entityManager = JpaUtil.getEntityManager();
            List<Room> available = entityManager.createQuery(
                            "SELECT r FROM Room r WHERE r.roomType = :t AND r.status = :s",
                            Room.class
                    )
                    .setParameter("t", roomType)
                    .setParameter("s", RoomStatus.AVAILABLE)
                    .getResultList();
            if (!available.isEmpty()) {
                return;
            }
            entityManager.getTransaction().begin();
            Room room = new RoomFactory().createRoom(roomType, "V" + (System.currentTimeMillis() % 100000), 9);
            entityManager.persist(room);
            entityManager.getTransaction().commit();
        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }
            throw new RuntimeException(e);
        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}
