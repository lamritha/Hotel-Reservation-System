package com.hotelreservation.events;

import com.hotelreservation.model.Room;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AvailabilityEventPublisher
        implements RoomAvailabilitySubject {

    private final List<RoomAvailabilityObserver> observers =
            new CopyOnWriteArrayList<>();

    @Override
    public void subscribe(RoomAvailabilityObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void unsubscribe(RoomAvailabilityObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void publish(RoomAvailabilityEvent event) {
        for (RoomAvailabilityObserver observer : observers) {
            observer.onRoomAvailable(event);
        }
    }

    public void roomBecameAvailable(
            Room room,
            LocalDate availableFrom
    ) {
        publish(
                new RoomAvailabilityEvent(
                        room,
                        availableFrom == null
                                ? LocalDate.now()
                                : availableFrom
                )
        );
    }
}
