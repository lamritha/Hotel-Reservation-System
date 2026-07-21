package com.hotelreservation.util;

import com.hotelreservation.model.Room;
import com.hotelreservation.model.RoomStatus;
import jakarta.persistence.EntityManager;

import java.util.List;

public class RoomStatusResetter {

    public static void main(String[] args) {
        EntityManager entityManager = null;

        try {
            entityManager = JpaUtil.getEntityManager();

            entityManager.getTransaction().begin();

            List<Room> rooms = entityManager
                    .createQuery("SELECT r FROM Room r", Room.class)
                    .getResultList();

            for (Room room : rooms) {
                room.setStatus(RoomStatus.AVAILABLE);
                entityManager.merge(room);
            }

            entityManager.getTransaction().commit();

            System.out.println("All rooms have been reset to AVAILABLE.");
            System.out.println("Rooms updated: " + rooms.size());

        } catch (Exception e) {
            if (entityManager != null && entityManager.getTransaction().isActive()) {
                entityManager.getTransaction().rollback();
            }

            System.out.println("Failed to reset room statuses.");
            e.printStackTrace();

        } finally {
            if (entityManager != null && entityManager.isOpen()) {
                entityManager.close();
            }

            JpaUtil.close();
        }
    }
}