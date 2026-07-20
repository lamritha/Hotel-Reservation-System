package com.hotelreservation.repository;

import com.hotelreservation.entity.Guest;

public class GuestRepository extends AbstractRepository<Guest> {

    public GuestRepository() {
        super(Guest.class);
    }

    public Guest save(Guest guest) {
        return persist(guest);
    }

    public Guest findById(Long guestId) {
        return find(guestId);
    }
}
