package com.hotelreservation.service;

import com.hotelreservation.entity.*;
import com.hotelreservation.factory.RoomFactory;
import com.hotelreservation.util.JpaUtil;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.util.List;

public class BookingServiceTest {

    public static void main(String[] args) {
        EntityManager entityManager = null;

        try {
            ensureAvailableRoom(RoomType.SINGLE);

            com.hotelreservation.util.BookingSession.setSingleRoomQuantity(1);
            com.hotelreservation.util.BookingSession.setDoubleRoomQuantity(0);
            com.hotelreservation.util.BookingSession.setDeluxeRoomQuantity(0);
            com.hotelreservation.util.BookingSession.setPenthouseRoomQuantity(0);

            BookingService bookingService = new BookingService();

            Guest guest = new Guest(
                    "Test",
                    "Guest",
                    "testguest" + System.currentTimeMillis() + "@email.com",
                    "416-555-0101",
                    "123 Test Street, Toronto"
            );

            Reservation reservation = bookingService.completeBooking(
                    guest,
                    LocalDate.of(2026, 7, 17),
                    LocalDate.of(2026, 7, 20),
                    1,
                    0,
                    false,
                    PaymentMethod.CARD
            );

            System.out.println("Booking completed successfully.");
            System.out.println("Reservation ID: " + reservation.getReservationId());
            System.out.println("Guest ID: " + reservation.getGuest().getGuestId());
            System.out.println("Room ID: " + reservation.getRoom().getRoomId());
            System.out.println("Room Type: " + reservation.getRoom().getRoomType());
            System.out.println("Reservation Status: " + reservation.getStatus());

            entityManager = JpaUtil.getEntityManager();

            Long guestCount = entityManager
                    .createQuery("SELECT COUNT(g) FROM Guest g", Long.class)
                    .getSingleResult();

            Long reservationCount = entityManager
                    .createQuery("SELECT COUNT(r) FROM Reservation r", Long.class)
                    .getSingleResult();

            Long billingCount = entityManager
                    .createQuery("SELECT COUNT(b) FROM Billing b", Long.class)
                    .getSingleResult();

            Long paymentCount = entityManager
                    .createQuery("SELECT COUNT(p) FROM Payment p", Long.class)
                    .getSingleResult();

            System.out.println("Guests in database: " + guestCount);
            System.out.println("Reservations in database: " + reservationCount);
            System.out.println("Billings in database: " + billingCount);
            System.out.println("Payments in database: " + paymentCount);

        } catch (Exception e) {
            System.out.println("Booking test failed.");
            e.printStackTrace();

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }

            JpaUtil.close();
        }
    }

    private static void ensureAvailableRoom(RoomType roomType) {
        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();

            List<Room> availableRooms = entityManager
                    .createQuery(
                            "SELECT r FROM Room r WHERE r.roomType = :roomType AND r.status = :status",
                            Room.class
                    )
                    .setParameter("roomType", roomType)
                    .setParameter("status", RoomStatus.AVAILABLE)
                    .getResultList();

            if (!availableRooms.isEmpty()) {
                return;
            }

            RoomFactory roomFactory = new RoomFactory();

            String roomNumber = "T" + System.currentTimeMillis() % 100000;
            Room room = roomFactory.createRoom(roomType, roomNumber, 9);

            entityManager.getTransaction().begin();
            entityManager.persist(room);
            entityManager.getTransaction().commit();

            System.out.println("Created extra available room for test: " + roomNumber);

        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            throw new RuntimeException("Failed to prepare available test room", e);

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }
        }
    }
}